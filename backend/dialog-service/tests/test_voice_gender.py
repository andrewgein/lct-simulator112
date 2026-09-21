import unittest

from app.domain.model import Gender
from app.adapter.out.processing.voice_profiles import get_voice_gender


class VoiceGenderTests(unittest.TestCase):
    def test_uses_child_voice_below_fourteen(self):
        self.assertEqual(get_voice_gender(13, Gender.MAN), "child")
        self.assertEqual(get_voice_gender(13, Gender.WOMEN), "child")

    def test_uses_gender_from_fourteen(self):
        self.assertEqual(get_voice_gender(14, Gender.MAN), "male")
        self.assertEqual(get_voice_gender(14, Gender.WOMEN), "female")


if __name__ == "__main__":
    unittest.main()
