import unittest

from app.domain.model import CallDirection, CallScenario, CounterpartyType, Gender, Person
from app.application.model.prompts import build_call_scenario


class CallScenarioPromptTests(unittest.TestCase):
    def test_builds_system112_caller_scenario(self):
        call = _call(person=Person(
            first_name="Анна",
            last_name="Иванова",
            middle_name="Сергеевна",
            address="Москва, улица Первая",
        ))

        prompt = build_call_scenario(call)

        self.assertIn("Собеседник: заявитель", prompt)
        self.assertIn("Фамилия: Иванова", prompt)
        self.assertIn("Имя: Анна", prompt)
        self.assertIn("Отчество: Сергеевна", prompt)
        self.assertIn("Адрес: Москва, улица Первая", prompt)
        self.assertIn("На вопрос «Как вас зовут?» или «Представьтесь»", prompt)
        self.assertIn("называй фамилию, имя и отчество (если указано)", prompt)
        self.assertNotIn("Полное имя для ответа оператору", prompt)
        self.assertNotIn("- Служба:", prompt)
        self.assertIn("Ты знаешь их к началу звонка", prompt)

    def test_builds_other_service_scenario(self):
        call = _call(counterparty=CounterpartyType.SERVICE,
                     direction=CallDirection.INBOUND,
                     service_code="AMBULANCE",
                     ai_context="Ты дежурный скорой помощи")

        prompt = build_call_scenario(call)

        self.assertIn("Собеседник: дежурный другой службы", prompt)
        self.assertIn("Направление: входящий", prompt)
        self.assertIn("Служба: AMBULANCE", prompt)
        self.assertIn("Ты дежурный скорой помощи", prompt)
        self.assertIn("СПЕЦИАЛЬНАЯ ЛОГИКА ПОВЕДЕНИЯ", prompt)
        self.assertIn("Она уточняет условия раскрытия фактов", prompt)
        self.assertIn("Это звонок от тебя в ДДС", prompt)
        self.assertIn("Никогда не угадывай и не произноси имя оператора", prompt)
        self.assertNotIn("ПРАВИЛА ПОВЕДЕНИЯ СОБЕСЕДНИКА", prompt)

    def test_builds_dds_brigade_scenario(self):
        call = _call(counterparty=CounterpartyType.BRIGADE,
                     direction=CallDirection.OUTBOUND,
                     known_facts=("Бригада прибыла на место",))

        prompt = build_call_scenario(call)

        self.assertIn("Собеседник: представитель бригады", prompt)
        self.assertIn("Направление: исходящий", prompt)
        self.assertIn("Бригада прибыла на место", prompt)
        self.assertIn("Это звонок оператора ДДС тебе", prompt)
        self.assertIn("Это справочник, а не текст доклада", prompt)
        self.assertIn("Если реплика непонятна", prompt)


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
