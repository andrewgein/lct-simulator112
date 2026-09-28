from dataclasses import dataclass
from hashlib import sha256
import io
import logging
import math
from os import getenv
import time
from threading import Lock
import wave

import httpx
import numpy as np

from app.adapter.out.processing.latency_tracker import tracker
from app.adapter.out.processing.voice_profiles import VOICE_PROFILES, VoiceProfile


OUTPUT_SAMPLE_RATE = 24000


@dataclass
class AudioChunk:
    audio: np.ndarray
    sample_rate: int = OUTPUT_SAMPLE_RATE


class TTSError(RuntimeError):
    """Raised when the F5-TTS server cannot synthesize speech."""


class TTSModel:
    """Singleton client for the F5-TTS Server HTTP API."""

    _instance = None
    _lock = Lock()

    def __new__(cls):
        with cls._lock:
            if cls._instance is None:
                instance = super().__new__(cls)
                instance._initialize()
                cls._instance = instance
            return cls._instance

    def _initialize(self) -> None:
        self.base_url = getenv("F5_TTS_BASE_URL", "").strip().rstrip("/")
        if self.base_url:
            url = httpx.URL(self.base_url)
            if (
                url.scheme not in ("http", "https")
                or not url.host
                or url.query
                or url.fragment
                or url.username
                or url.password
            ):
                raise ValueError(
                    "F5_TTS_BASE_URL must be an HTTP(S) server URL without "
                    "credentials or query parameters"
                )

        read_timeout = float(getenv("F5_TTS_READ_TIMEOUT", "120"))
        if not math.isfinite(read_timeout) or read_timeout <= 0:
            raise ValueError("F5_TTS_READ_TIMEOUT must be positive and finite")
        self.timeout = httpx.Timeout(read_timeout, connect=5.0, pool=5.0)

        self.text_preprocessor = None
        self._registration_lock = Lock()
        self._registered_voices: set[str] = set()
        if not self.base_url:
            logging.getLogger(__name__).warning(
                "F5_TTS_BASE_URL is not configured; speech synthesis is unavailable"
            )

    def register_all_voices(self) -> None:
        """Preloads reference audio for every voice profile into the F5-TTS"""
        if not self.base_url:
            return

        with httpx.Client(base_url=self.base_url + "/", timeout=self.timeout) as client:
            for profile in VOICE_PROFILES:
                reference_audio = profile.audio_path.read_bytes()
                voice = self._voice_name(profile, reference_audio)
                self._register_voice(client, profile, reference_audio, voice)

    def load_text_preprocessor(self):
        with self._lock:
            if self.text_preprocessor is None:
                from app.adapter.out.processing.tts_text_preprocessor import TTSTextPreprocessor

                self.text_preprocessor = TTSTextPreprocessor()
            return self.text_preprocessor

    def generate(self, text: str, profile: VoiceProfile):
        prepared_text = self.load_text_preprocessor().process(text)

        yield from self._generate_audio(prepared_text, profile)

    @staticmethod
    def _voice_name(profile: VoiceProfile, audio: bytes) -> str:
        # A content-derived name lets the F5 server keep old and updated versions of
        # the same profile distinct.
        digest = sha256(audio).hexdigest()[:16]
        return f"sim112-{profile.id}-{digest}"

    def _register_voice(
        self, client: httpx.Client, profile: VoiceProfile, audio: bytes, voice: str
    ) -> None:
        with self._registration_lock:
            if voice in self._registered_voices:
                return

            suffix = profile.audio_path.suffix.lower() or ".wav"
            response = client.post(
                "upload_audio/",
                data={"audio_file_label": voice},
                files={
                    "file": (
                        f"reference{suffix}",
                        audio,
                        "audio/wav" if suffix == ".wav" else "application/octet-stream",
                    )
                },
            )
            response.raise_for_status()
            payload = response.json()
            if not isinstance(payload, dict) or payload.get("error"):
                detail = (
                    payload.get("error", "invalid registration response")
                    if isinstance(payload, dict)
                    else "invalid registration response"
                )
                raise TTSError(f"F5-TTS voice registration failed: {detail}")
            self._registered_voices.add(voice)

    def _generate_audio(self, text: str, profile: VoiceProfile):
        if not self.base_url:
            raise TTSError("F5_TTS_BASE_URL is not configured")

        try:
            reference_audio = profile.audio_path.read_bytes()
            voice = self._voice_name(profile, reference_audio)

            with httpx.Client(
                base_url=self.base_url + "/", timeout=self.timeout
            ) as client:
                self._register_voice(client, profile, reference_audio, voice)
                request_started = time.monotonic()
                response = client.get(
                    "synthesize_speech/",
                    params={"text": text, "voice": voice},
                )
                response.raise_for_status()
                tracker.record("tts", time.monotonic() - request_started)
                content_type = (
                    response.headers.get("content-type", "")
                    .split(";")[0]
                    .strip()
                    .lower()
                )
                if content_type not in {"audio/wav", "audio/x-wav", "audio/wave"}:
                    raise TTSError(
                        f"Expected audio/wav, received "
                        f"{content_type or 'no Content-Type'}"
                    )

                audio, sample_rate = self._decode_wav(response.content)
                if not audio.size:
                    raise TTSError("F5-TTS server returned no audio")
                yield AudioChunk(audio=audio, sample_rate=sample_rate)
        except TTSError:
            raise
        except httpx.TimeoutException as exc:
            raise TTSError(
                "F5-TTS server timed out; check availability and "
                "F5_TTS_READ_TIMEOUT"
            ) from exc
        except httpx.HTTPStatusError as exc:
            raise TTSError(
                f"F5-TTS server returned HTTP {exc.response.status_code} "
                f"for {exc.request.url.path}"
            ) from exc
        except httpx.RequestError as exc:
            raise TTSError(
                f"F5-TTS connection failed ({type(exc).__name__}); check "
                "F5_TTS_BASE_URL and server availability"
            ) from exc
        except (OSError, ValueError, KeyError, TypeError, wave.Error) as exc:
            raise TTSError(
                f"Invalid TTS voice file or server response "
                f"({type(exc).__name__})"
            ) from exc

    @staticmethod
    def _decode_wav(data: bytes) -> tuple[np.ndarray, int]:
        with wave.open(io.BytesIO(data), "rb") as wav_file:
            if wav_file.getnchannels() != 1:
                raise ValueError("F5-TTS response must be mono")
            sample_rate = wav_file.getframerate()
            sample_width = wav_file.getsampwidth()
            frames = wav_file.readframes(wav_file.getnframes())

        if sample_width == 1:
            audio = (np.frombuffer(frames, dtype=np.uint8).astype(np.float32) - 128) / 128
        elif sample_width == 2:
            audio = np.frombuffer(frames, dtype="<i2").astype(np.float32) / 32768
        elif sample_width == 3:
            raw = np.frombuffer(frames, dtype=np.uint8).reshape(-1, 3)
            values = (
                raw[:, 0].astype(np.int32)
                | (raw[:, 1].astype(np.int32) << 8)
                | (raw[:, 2].astype(np.int32) << 16)
            )
            values = np.where(values & 0x800000, values - 0x1000000, values)
            audio = values.astype(np.float32) / 8388608
        elif sample_width == 4:
            audio = np.frombuffer(frames, dtype="<i4").astype(np.float32) / 2147483648
        else:
            raise ValueError(f"Unsupported WAV sample width: {sample_width}")

        return audio, sample_rate
