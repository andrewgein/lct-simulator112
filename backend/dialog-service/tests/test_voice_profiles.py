import json
import os
import unittest
from unittest.mock import patch

from app.grpc.com.simulator112.incident.incident_context_pb2 import (
    CALM,
    PANICKED,
)
from app.utils.voice_profiles import (
    VOICE_PROFILES,
    VOICES_ROOT,
    get_voice_profile,
    select_voice_profile,
    validate_voice_profiles,
)


class VoiceManifestTests(unittest.TestCase):
    def test_loads_every_voice_and_emotion(self):
        voice_directories = [
            path
            for category in ("man", "woman", "children")
            for path in (VOICES_ROOT / category).iterdir()
            if path.is_dir()
        ]

        self.assertEqual(len(VOICE_PROFILES), len(voice_directories) * 5)
        self.assertEqual(len({profile.id for profile in VOICE_PROFILES}), len(VOICE_PROFILES))
        validate_voice_profiles()

    def test_inherits_root_reference_text(self):
        defaults = json.loads(
            (VOICES_ROOT / "manifest.json").read_text(encoding="utf-8")
        )
        profile = get_voice_profile("man-fedya-calm")

        self.assertEqual(
            profile.reference_text,
            defaults["emotions"]["calm"]["ref_text"],
        )

    def test_child_manifest_overrides_reference_text(self):
        manifest_path = VOICES_ROOT / "children" / "jenya" / "manifest.json"
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
        profile = get_voice_profile("child-jenya-calm")

        self.assertEqual(
            profile.reference_text,
            manifest["emotions"]["calm"]["ref_text"],
        )

    def test_selects_requested_gender_and_emotion(self):
        with patch.dict(os.environ, {}, clear=False):
            os.environ.pop("TTS_VOICE_PROFILE", None)
            profile = select_voice_profile(PANICKED, gender="female")

        self.assertEqual(profile.gender, "female")
        self.assertEqual(profile.emotional_states, (PANICKED,))

    def test_fixed_profile_id(self):
        with patch.dict(
            os.environ,
            {"TTS_VOICE_PROFILE": "man-artyom-calm"},
            clear=False,
        ):
            profile = select_voice_profile(CALM)

        self.assertEqual(profile.id, "man-artyom-calm")
        self.assertEqual(profile.audio_path.name, "calm.wav")


if __name__ == "__main__":
    unittest.main()
