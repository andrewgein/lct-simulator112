import unittest

from app.domain.model import CallDirection, CallScenario, CounterpartyType, Gender, Person
from app.application.model.prompts import BRIGADE_INBOUND_SYSTEM_PROMPT, BRIGADE_OUTBOUND_SYSTEM_PROMPT, BRIGADE_SYSTEM_PROMPT, CALLER_SYSTEM_PROMPT, SERVICE_SYSTEM_PROMPT, build_call_scenario


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
        self.assertIn("ты знаешь их к началу звонка", prompt)

    def test_address_speech_rules_apply_to_all_roles(self):
        for system_prompt in (CALLER_SYSTEM_PROMPT, BRIGADE_INBOUND_SYSTEM_PROMPT,
                              BRIGADE_OUTBOUND_SYSTEM_PROMPT, SERVICE_SYSTEM_PROMPT):
            with self.subTest(role=system_prompt[:40]):
                self.assertIn('«дом»', system_prompt)
                self.assertIn('«квартира»', system_prompt)
                self.assertIn('не ставь точки внутри адреса', system_prompt)
                self.assertIn('не добавляй отсутствующие части' if system_prompt != CALLER_SYSTEM_PROMPT
                              else 'не дополняй адрес догадками', system_prompt)

    def test_caller_hidden_fact_is_absent_from_opening_prompt(self):
        call = _call(hidden_facts=("Секрет о дыхании: после вопроса о дыхании",))
        opening = build_call_scenario(call, include_hidden=False)
        continued = build_call_scenario(call)

        self.assertNotIn("Секрет о дыхании", opening)
        self.assertIn("Секрет о дыхании", continued)

    def test_builds_other_service_scenario(self):
        call = _call(counterparty=CounterpartyType.SERVICE,
                     direction=CallDirection.INBOUND,
                     service_code="AMBULANCE",
                     ai_context="Ты дежурный скорой помощи")

        prompt = build_call_scenario(call)

        self.assertIn("Собеседник: дежурный другой службы", prompt)
        self.assertIn("Направление: входящий в ДДС", prompt)
        self.assertIn("Служба: AMBULANCE", prompt)
        self.assertIn("Ты дежурный скорой помощи", prompt)
        self.assertIn("Дополнительная логика: Ты дежурный скорой помощи", prompt)
        self.assertIn("начинай разговор первым после соединения", prompt)
        self.assertNotIn("должность и фамилию", prompt)

    def test_builds_dds_brigade_scenario(self):
        call = _call(counterparty=CounterpartyType.BRIGADE,
                     direction=CallDirection.OUTBOUND,
                     known_facts=("Бригада прибыла на место",))

        prompt = build_call_scenario(call)

        self.assertIn("Собеседник: сотрудник выездной бригады", prompt)
        self.assertIn("Направление: исходящий из ДДС", prompt)
        self.assertIn("Бригада прибыла на место", prompt)
        self.assertIn("Тебе звонит оператор", prompt)
        self.assertIn("бригада ещё не на месте", prompt)
        self.assertIn("Если по фактам бригада прибыла, отвечай о работе на месте", prompt)

    def test_dds_roles_have_distinct_rules_for_greeting_question_and_goodbye(self):
        for counterparty, system_prompt in ((CounterpartyType.BRIGADE, BRIGADE_SYSTEM_PROMPT),
                                            (CounterpartyType.SERVICE, SERVICE_SYSTEM_PROMPT)):
            inbound = build_call_scenario(_call(counterparty=counterparty, direction=CallDirection.INBOUND))
            outbound = build_call_scenario(_call(counterparty=counterparty, direction=CallDirection.OUTBOUND))
            self.assertIn("начинай разговор первым", inbound)
            self.assertIn("Тебе звонит оператор", outbound)
            self.assertIn("прощ", system_prompt.lower())
            self.assertIn("срок", system_prompt)

    def test_arrived_brigade_reports_status_instead_of_dispatching_again(self):
        call = _call(counterparty=CounterpartyType.BRIGADE, direction=CallDirection.INBOUND,
                     known_facts=("Расчёт прибыл во двор", "Из двух окон идёт чёрный дым"),
                     hidden_facts=("Нужен второй расчёт: только после вопроса об усилении",),
                     ai_context="Сначала сообщи о прибытии и дыме")
        prompt = BRIGADE_INBOUND_SYSTEM_PROMPT + "\n" + build_call_scenario(call)

        self.assertIn("Расчёт прибыл во двор", prompt)
        self.assertIn("чёрный дым", prompt)
        self.assertNotIn("выезжаем", prompt.lower())
        self.assertNotIn("Слушаю вас", prompt)
        self.assertIn("Спасибо, до свидания", prompt)
        self.assertIn("уже на месте", BRIGADE_OUTBOUND_SYSTEM_PROMPT)

    def test_dds_address_is_only_from_call_person(self):
        empty = build_call_scenario(_call(counterparty=CounterpartyType.BRIGADE))
        filled = build_call_scenario(_call(counterparty=CounterpartyType.SERVICE,
                                           person=Person(address="Москва, Тверская, 8")))

        self.assertIn("Адрес: не указан", empty)
        self.assertNotIn("АДРЕС ПРОИСШЕСТВИЯ ТЕКУЩЕГО ЭТАПА ДДС", empty)
        self.assertIn("Адрес: Москва, Тверская, 8", filled)

    def test_incoming_brigade_starts_before_operator(self):
        prompt = build_call_scenario(_call(counterparty=CounterpartyType.BRIGADE, direction=CallDirection.INBOUND))

        self.assertIn("начинай разговор первым после соединения", prompt)
        self.assertIn("текущий статус только по известным фактам", prompt)
        self.assertIn("не раскрывай скрытые факты во вступлении", prompt)


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
