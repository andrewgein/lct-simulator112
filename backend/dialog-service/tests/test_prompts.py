import unittest

from app.grpc.com.simulator112.incident.incident_context_pb2 import Applicant, DialupContext
from app.prompts import build_dialup_scenario


class DialupScenarioTests(unittest.TestCase):
    def test_distinguishes_applicant_location_from_victim_incident_address(self):
        dialup = DialupContext(
            id="dialup-1",
            applicant=Applicant(
                first_name="Анна",
                address="Москва, улица Первая, дом 1",
            ),
            victim=Applicant(
                first_name="Иван",
                address="Москва, улица Вторая, дом 2",
            ),
        )
        prompt = build_dialup_scenario(dialup)

        self.assertIn("Текущее местонахождение заявителя: Москва, улица Первая, дом 1", prompt)
        self.assertIn(
            "Адрес происшествия / местонахождения пострадавшего: "
            "Москва, улица Вторая, дом 2",
            prompt,
        )
        self.assertIn("Местонахождение заявителя и адрес происшествия различаются", prompt)

    def test_marks_equal_participant_addresses(self):
        address = "Москва, улица Общая, дом 10"
        dialup = DialupContext(
            applicant=Applicant(address=address),
            victim=Applicant(address=address.upper()),
        )

        prompt = build_dialup_scenario(dialup)

        self.assertIn("Местонахождение заявителя совпадает с адресом происшествия", prompt)


if __name__ == "__main__":
    unittest.main()
