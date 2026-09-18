import unittest

from app.grpc.com.simulator112.incident.incident_context_pb2 import MAN, WOMEN
from app.utils.voice_profiles import get_voice_gender


class VoiceGenderTests(unittest.TestCase):
    def test_uses_child_voice_below_fourteen(self):
        self.assertEqual(get_voice_gender(13, MAN), "child")
        self.assertEqual(get_voice_gender(13, WOMEN), "child")

    def test_uses_gender_from_fourteen(self):
        self.assertEqual(get_voice_gender(14, MAN), "male")
        self.assertEqual(get_voice_gender(14, WOMEN), "female")


if __name__ == "__main__":
    unittest.main()
