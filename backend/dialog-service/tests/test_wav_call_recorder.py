import io
import unittest
import wave
from unittest.mock import MagicMock, patch

import numpy as np

from app.adapter.out.recording.wav_call_recorder import S3CallRecorderFactory, WavCallRecorder


class WavCallRecorderTests(unittest.TestCase):
    def test_writes_operator_and_counterparty_to_separate_stereo_channels(self):
        saved = []
        recorder = WavCallRecorder(
            "s3://recordings/call.wav",
            saved.append,
            operator_sample_rate=24000,
            counterparty_sample_rate=24000,
            output_sample_rate=24000,
        )
        operator = np.array([100, 200, 300], dtype="<i2")
        counterparty = np.array([-100, -200], dtype="<i2")

        with patch(
            "app.adapter.out.recording.wav_call_recorder.time.monotonic",
            return_value=recorder._started_at,
        ):
            recorder.record_operator(operator.tobytes())
            recorder.record_counterparty(counterparty.tobytes())
            result = recorder.close()

        self.assertEqual("s3://recordings/call.wav", result)
        with wave.open(io.BytesIO(saved[0]), "rb") as recording:
            self.assertEqual(2, recording.getnchannels())
            self.assertEqual(24000, recording.getframerate())
            frames = np.frombuffer(
                recording.readframes(recording.getnframes()), dtype="<i2"
            ).reshape(-1, 2)
        np.testing.assert_array_equal(operator, frames[:3, 0])
        np.testing.assert_array_equal(counterparty, frames[:2, 1])

    def test_does_not_upload_an_empty_recording(self):
        saved = []
        recorder = WavCallRecorder("s3://recordings/empty.wav", saved.append)

        with patch(
            "app.adapter.out.recording.wav_call_recorder.time.monotonic",
            return_value=recorder._started_at,
        ):
            result = recorder.close()

        self.assertIsNone(result)
        self.assertEqual([], saved)

    def test_preserves_silence_until_the_call_is_dropped(self):
        saved = []
        recorder = WavCallRecorder(
            "s3://recordings/call.wav", saved.append, output_sample_rate=24000
        )
        with patch(
            "app.adapter.out.recording.wav_call_recorder.time.monotonic",
            return_value=recorder._started_at,
        ):
            recorder.record_counterparty(np.array([100], dtype="<i2").tobytes())
        with patch(
            "app.adapter.out.recording.wav_call_recorder.time.monotonic",
            return_value=recorder._started_at + 2,
        ):
            recorder.close()

        with wave.open(io.BytesIO(saved[0]), "rb") as recording:
            self.assertEqual(48000, recording.getnframes())

    def test_factory_uploads_to_sanitized_s3_key(self):
        client = MagicMock()
        recorder = S3CallRecorderFactory(client, "call-recordings").create(
            "context/../../one", "call two"
        )
        with patch(
            "app.adapter.out.recording.wav_call_recorder.time.monotonic",
            return_value=recorder._started_at,
        ):
            recorder.record_operator(np.array([100], dtype="<i2").tobytes())
            result = recorder.close()

        client.head_bucket.assert_called_once_with(Bucket="call-recordings")
        request = client.put_object.call_args.kwargs
        self.assertEqual("call-recordings", request["Bucket"])
        key_parts = request["Key"].split("/")
        self.assertEqual("recordings", key_parts[0])
        self.assertNotIn("..", key_parts[1])
        self.assertEqual("call_two", key_parts[2])
        self.assertTrue(request["Key"].endswith(".wav"))
        self.assertEqual("audio/wav", request["ContentType"])
        self.assertTrue(request["Body"].startswith(b"RIFF"))
        self.assertEqual(f"s3://call-recordings/{request['Key']}", result)


def _wav(samples):
    output = io.BytesIO()
    with wave.open(output, "wb") as recording:
        recording.setnchannels(2)
        recording.setsampwidth(2)
        recording.setframerate(24000)
        recording.writeframes(np.array(samples, dtype="<i2").tobytes())
    return output.getvalue()


class SingleRecordingPerCallTests(unittest.TestCase):
    def _close_recorder(self, client, restart=False):
        recorder = S3CallRecorderFactory(client, "call-recordings").create("ctx", "call", restart)
        with patch(
            "app.adapter.out.recording.wav_call_recorder.time.monotonic",
            return_value=recorder._started_at,
        ):
            recorder.record_operator(np.array([7], dtype="<i2").tobytes())
            recorder.close()

    def test_reconnect_appends_to_existing_recording(self):
        client = MagicMock()
        old_key = "recordings/ctx/call/20260101T000000_000000Z.wav"
        client.list_objects_v2.return_value = {"Contents": [{"Key": old_key}]}
        client.get_object.return_value = {"Body": io.BytesIO(_wav([1, 2, 3, 4]))}

        self._close_recorder(client)

        request = client.put_object.call_args.kwargs
        self.assertEqual(old_key, request["Key"])
        client.delete_object.assert_not_called()
        with wave.open(io.BytesIO(request["Body"]), "rb") as merged:
            frames = np.frombuffer(merged.readframes(merged.getnframes()), dtype="<i2").reshape(-1, 2)
        np.testing.assert_array_equal([[1, 2], [3, 4]], frames[:2])
        self.assertGreater(len(frames), 2)

    def test_restart_replaces_existing_recordings(self):
        client = MagicMock()
        old_key = "recordings/ctx/call/20260101T000000_000000Z.wav"
        client.list_objects_v2.return_value = {"Contents": [{"Key": old_key}]}

        self._close_recorder(client, restart=True)

        client.delete_object.assert_called_once_with(Bucket="call-recordings", Key=old_key)
        self.assertNotEqual(old_key, client.put_object.call_args.kwargs["Key"])
        client.get_object.assert_not_called()


if __name__ == "__main__":
    unittest.main()
