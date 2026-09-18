from hashlib import sha256
import io
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import wave

import httpx
import numpy as np
from dotenv import load_dotenv

from app.utils.tts_model import TTSError, TTSModel
from app.utils.voice_profiles import VoiceProfile, get_voice_profile


class _IdentityPreprocessor:
    def process(self, text):
        return text


def _wav_bytes(samples, sample_rate=24000):
    output = io.BytesIO()
    with wave.open(output, "wb") as wav_file:
        wav_file.setnchannels(1)
        wav_file.setsampwidth(2)
        wav_file.setframerate(sample_rate)
        wav_file.writeframes(np.asarray(samples, dtype="<i2").tobytes())
    return output.getvalue()


class TTSModelTests(unittest.TestCase):
    def setUp(self):
        TTSModel._instance = None
        self.env = patch.dict(
            os.environ,
            {
                "F5_TTS_BASE_URL": "http://localhost:7860",
                "F5_TTS_READ_TIMEOUT": "30",
            },
            clear=False,
        )
        self.env.start()
        self.addCleanup(self.env.stop)
        self.addCleanup(setattr, TTSModel, "_instance", None)

        temporary_directory = tempfile.TemporaryDirectory()
        self.addCleanup(temporary_directory.cleanup)
        self.reference_wav = _wav_bytes([0, 100, -100])
        audio_path = Path(temporary_directory.name) / "reference.wav"
        audio_path.write_bytes(self.reference_wav)
        self.profile = VoiceProfile(
            id="test-voice",
            audio_path=audio_path,
            reference_text="Текст образца",
            gender="female",
            age_group="adult",
            emotional_states=(),
        )

    def _model(self):
        model = TTSModel()
        model.text_preprocessor = _IdentityPreprocessor()
        return model

    @staticmethod
    def _client_using(transport):
        real_client = httpx.Client

        def build_client(*args, **kwargs):
            return real_client(*args, transport=transport, **kwargs)

        return patch("app.utils.tts_model.httpx.Client", side_effect=build_client)

    def test_registers_voice_then_synthesizes_with_its_label(self):
        requests = []
        output_samples = np.array([-32768, -1, 0, 16384, 32767], dtype="<i2")

        def handler(request):
            requests.append(request)
            if request.url.path == "/upload_audio/":
                return httpx.Response(200, json={"message": "uploaded"})
            return httpx.Response(
                200,
                headers={"content-type": "audio/wav"},
                content=_wav_bytes(output_samples),
            )

        with self._client_using(httpx.MockTransport(handler)):
            chunks = list(self._model().generate("Проверка", self.profile))

        digest = sha256(self.reference_wav).hexdigest()[:16]
        voice = f"sim112-{self.profile.id}-{digest}"
        self.assertEqual(
            [(request.method, request.url.path) for request in requests],
            [("POST", "/upload_audio/"), ("GET", "/synthesize_speech/")],
        )
        registration = requests[0].content.decode("utf-8", errors="ignore")
        self.assertIn('name="audio_file_label"', registration)
        self.assertIn(voice, registration)
        self.assertIn('filename="reference.wav"', registration)
        self.assertIn(self.reference_wav, requests[0].content)

        query = dict(requests[1].url.params)
        self.assertEqual(
            query,
            {"text": "Проверка", "voice": voice},
        )
        self.assertEqual(len(chunks), 1)
        self.assertEqual(chunks[0].sample_rate, 24000)
        np.testing.assert_allclose(
            chunks[0].audio,
            output_samples.astype(np.float32) / 32768,
        )

    def test_registers_a_voice_only_once_for_multiple_responses(self):
        methods = []

        def handler(request):
            methods.append((request.method, request.url.path))
            if request.url.path == "/upload_audio/":
                return httpx.Response(200, json={"message": "uploaded"})
            return httpx.Response(
                200,
                headers={"content-type": "audio/x-wav"},
                content=_wav_bytes([0]),
            )

        with self._client_using(httpx.MockTransport(handler)):
            model = self._model()
            list(model.generate("Первый ответ", self.profile))
            list(model.generate("Второй ответ", self.profile))

        self.assertEqual(
            methods,
            [
                ("POST", "/upload_audio/"),
                ("GET", "/synthesize_speech/"),
                ("GET", "/synthesize_speech/"),
            ],
        )

    def test_does_not_cache_failed_registration(self):
        calls = 0

        def handler(request):
            nonlocal calls
            calls += 1
            return httpx.Response(200, json={"error": "Invalid file"})

        with self._client_using(httpx.MockTransport(handler)):
            model = self._model()
            for _ in range(2):
                with self.assertRaisesRegex(TTSError, "registration failed"):
                    list(model.generate("Тест", self.profile))

        self.assertEqual(calls, 2)

    def test_rejects_invalid_wav_response(self):
        def handler(request):
            if request.url.path == "/upload_audio/":
                return httpx.Response(200, json={"message": "uploaded"})
            return httpx.Response(
                200,
                headers={"content-type": "audio/wav"},
                content=b"not a wav",
            )

        with self._client_using(httpx.MockTransport(handler)):
            with self.assertRaisesRegex(TTSError, "Invalid TTS.*server response"):
                list(self._model().generate("Тест", self.profile))

    def test_wraps_server_http_error(self):
        transport = httpx.MockTransport(
            lambda request: httpx.Response(503, request=request)
        )
        with self._client_using(transport):
            with self.assertRaisesRegex(TTSError, "HTTP 503.*upload_audio"):
                list(self._model().generate("Тест", self.profile))

    def test_requires_server_url(self):
        os.environ["F5_TTS_BASE_URL"] = ""
        model = self._model()
        with self.assertRaisesRegex(TTSError, "F5_TTS_BASE_URL is not configured"):
            list(model.generate("Тест", self.profile))


@unittest.skipUnless(
    os.getenv("F5_TTS_INTEGRATION_TEST") == "1",
    "set F5_TTS_INTEGRATION_TEST=1 to test a running F5-TTS server",
)
class F5TTSIntegrationTests(unittest.TestCase):
    def setUp(self):
        TTSModel._instance = None
        self.addCleanup(setattr, TTSModel, "_instance", None)
        load_dotenv(Path(__file__).resolve().parents[1] / ".env")
        os.environ.setdefault("F5_TTS_BASE_URL", "http://127.0.0.1:7860")

    def test_running_server_synthesizes_wav(self):
        model = TTSModel()
        model.text_preprocessor = _IdentityPreprocessor()
        profile = get_voice_profile("man-artyom-calm")

        chunks = list(model.generate("Проверка синтеза речи.", profile))

        self.assertGreater(sum(chunk.audio.size for chunk in chunks), 0)
        self.assertTrue(all(chunk.sample_rate == 24000 for chunk in chunks))
        self.assertTrue(all(chunk.audio.dtype == np.float32 for chunk in chunks))
        self.assertTrue(all(np.isfinite(chunk.audio).all() for chunk in chunks))
        self.assertTrue(all(np.abs(chunk.audio).max(initial=0) <= 1 for chunk in chunks))


if __name__ == "__main__":
    unittest.main()
