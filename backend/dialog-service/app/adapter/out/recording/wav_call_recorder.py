import io
import logging
import os
import re
import threading
import time
import wave
from collections.abc import Callable
from datetime import datetime, timezone

import boto3
import numpy as np
from botocore.config import Config
from botocore.exceptions import ClientError

logger = logging.getLogger(__name__)
SAFE_FILE_PART = re.compile(r"[^a-zA-Z0-9_.-]+")


def _safe_file_part(value: str) -> str:
    sanitized = SAFE_FILE_PART.sub("_", value)
    return re.sub(r"\.{2,}", "_", sanitized).strip("._") or "unknown"


def _resample(samples: np.ndarray, source_rate: int, target_rate: int) -> np.ndarray:
    if not samples.size or source_rate == target_rate:
        return samples
    target_size = max(1, round(samples.size * target_rate / source_rate))
    positions = np.linspace(0, samples.size - 1, target_size)
    resampled = np.interp(positions, np.arange(samples.size), samples.astype(np.float32))
    return np.clip(resampled, -32768, 32767).astype(np.int16)


class WavCallRecorder:
    def __init__(
        self,
        destination: str,
        save: Callable[[bytes], None],
        operator_sample_rate: int = 44100,
        counterparty_sample_rate: int = 24000,
        output_sample_rate: int = 24000,
    ):
        self._destination = destination
        self._save = save
        self._operator_sample_rate = operator_sample_rate
        self._counterparty_sample_rate = counterparty_sample_rate
        self._output_sample_rate = output_sample_rate
        self._started_at = time.monotonic()
        self._chunks: dict[str, list[tuple[int, np.ndarray]]] = {
            "operator": [],
            "counterparty": [],
        }
        self._positions = {"operator": 0, "counterparty": 0}
        self._closed = False
        self._lock = threading.Lock()

    def record_operator(self, pcm: bytes) -> None:
        self._record("operator", pcm, self._operator_sample_rate)

    def record_counterparty(self, pcm: bytes) -> None:
        self._record("counterparty", pcm, self._counterparty_sample_rate)

    def _record(self, channel: str, pcm: bytes, sample_rate: int) -> None:
        if not pcm:
            return
        samples = np.frombuffer(pcm, dtype="<i2")
        samples = _resample(samples, sample_rate, self._output_sample_rate)
        with self._lock:
            if self._closed:
                return
            elapsed_position = round(
                (time.monotonic() - self._started_at) * self._output_sample_rate
            )
            position = max(elapsed_position, self._positions[channel])
            self._chunks[channel].append((position, samples.copy()))
            self._positions[channel] = position + samples.size

    def close(self) -> str | None:
        with self._lock:
            if self._closed:
                return self._destination
            self._closed = True
            elapsed_frames = round(
                (time.monotonic() - self._started_at) * self._output_sample_rate
            )
            chunks = {channel: list(values) for channel, values in self._chunks.items()}

        frame_count = max(
            elapsed_frames,
            max(
                (
                    position + samples.size
                    for values in chunks.values()
                    for position, samples in values
                ),
                default=0,
            ),
        )
        if frame_count == 0:
            logger.info("Call recording is empty, no S3 object written: %s", self._destination)
            return None

        operator = np.zeros(frame_count, dtype=np.int16)
        counterparty = np.zeros(frame_count, dtype=np.int16)
        for position, samples in chunks["operator"]:
            operator[position:position + samples.size] = samples
        for position, samples in chunks["counterparty"]:
            counterparty[position:position + samples.size] = samples

        stereo = np.column_stack((operator, counterparty)).astype("<i2", copy=False)
        output = io.BytesIO()
        with wave.open(output, "wb") as recording:
            recording.setnchannels(2)
            recording.setsampwidth(2)
            recording.setframerate(self._output_sample_rate)
            recording.writeframes(stereo.tobytes())
        self._save(output.getvalue())
        logger.info("Call recording saved to %s", self._destination)
        return self._destination


class S3CallRecorderFactory:
    def __init__(self, client=None, bucket: str | None = None):
        self._bucket = bucket or os.getenv("S3_RECORDINGS_BUCKET", "call-recordings")
        self._client = client or boto3.client(
            "s3",
            endpoint_url=os.getenv("S3_ENDPOINT", "http://127.0.0.1:9000"),
            aws_access_key_id=os.getenv("S3_ACCESS_KEY", "minioadmin"),
            aws_secret_access_key=os.getenv("S3_SECRET_KEY", "minioadmin"),
            region_name=os.getenv("S3_REGION", "us-east-1"),
            config=Config(s3={"addressing_style": "path"}),
        )
        self._bucket_ready = False
        self._bucket_lock = threading.Lock()

    def create(self, context_id: str, call_id: str) -> WavCallRecorder:
        timestamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S_%fZ")
        safe_context_id = _safe_file_part(context_id)
        safe_call_id = _safe_file_part(call_id)
        object_key = f"recordings/{safe_context_id}/{safe_call_id}/{timestamp}.wav"
        destination = f"s3://{self._bucket}/{object_key}"
        return WavCallRecorder(
            destination,
            lambda content: self._upload(object_key, content),
        )

    def _upload(self, object_key: str, content: bytes) -> None:
        self._ensure_bucket()
        self._client.put_object(
            Bucket=self._bucket,
            Key=object_key,
            Body=content,
            ContentType="audio/wav",
        )

    def _ensure_bucket(self) -> None:
        if self._bucket_ready:
            return
        with self._bucket_lock:
            if self._bucket_ready:
                return
            try:
                self._client.head_bucket(Bucket=self._bucket)
            except ClientError as error:
                code = error.response.get("Error", {}).get("Code")
                if code not in {"404", "NoSuchBucket", "NotFound"}:
                    raise
                self._client.create_bucket(Bucket=self._bucket)
            self._bucket_ready = True
