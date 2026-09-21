import unittest

from app.domain.model import CallDirection, CallScenario, CounterpartyType, Gender, Person
from app.application.model.prompts import build_call_scenario


class CallScenarioPromptTests(unittest.TestCase):
    def test_builds_system112_caller_scenario(self):
        call = _call(person=Person(first_name="Анна", address="Москва, улица Первая"))

        prompt = build_call_scenario(call)

        self.assertIn("Собеседник: заявитель", prompt)
        self.assertIn("Имя: Анна", prompt)
        self.assertIn("Адрес: Москва, улица Первая", prompt)

    def test_builds_dds_brigade_scenario(self):
        call = _call(counterparty=CounterpartyType.BRIGADE,
                     direction=CallDirection.OUTBOUND,
                     known_facts=("Бригада прибыла на место",))

        prompt = build_call_scenario(call)

        self.assertIn("Собеседник: представитель бригады", prompt)
        self.assertIn("Направление: исходящий", prompt)
        self.assertIn("Бригада прибыла на место", prompt)


def _call(**changes) -> CallScenario:
    values = dict(
        id="call-1", position=1, direction=CallDirection.INBOUND,
        counterparty=CounterpartyType.CALLER, person=Person(), gender=Gender.WOMEN,
        known_facts=(), hidden_facts=(), ai_context="", emotional_state="WORRIED",
    )
    values.update(changes)
    return CallScenario(**values)


if __name__ == "__main__":
    unittest.main()
