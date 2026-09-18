import unittest
from unittest.mock import patch

from app.grpc.com.simulator112.incident.incident_context_pb2 import (
    Applicant,
    DialupContext,
)
from app.utils.context_service import get_dialup


class DialupContextLookupTests(unittest.TestCase):
    @patch("app.utils.context_service._call")
    def test_requests_dialup_with_its_victim(self, call_mock):
        applicant = Applicant(
            first_name="Анна",
            address="Москва, улица Заявителя, дом 1",
        )
        victim = Applicant(
            first_name="Иван",
            address="Москва, улица Происшествия, дом 2",
        )
        call_mock.return_value = DialupContext(
            id="dialup-1",
            applicant=applicant,
            victim=victim,
        )

        found_dialup = get_dialup("context-1", "dialup-1")

        self.assertEqual("Москва, улица Заявителя, дом 1", found_dialup.applicant.address)
        self.assertEqual("Москва, улица Происшествия, дом 2", found_dialup.victim.address)
        method, request = call_mock.call_args.args
        self.assertEqual("GetDialup", method)
        self.assertEqual("context-1", request.context_id)
        self.assertEqual("dialup-1", request.dialup_id)


if __name__ == "__main__":
    unittest.main()
