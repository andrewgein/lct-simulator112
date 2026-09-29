import unittest

from app.adapter.out.processing.llm_model import LLMModel


class LlmContextBudgetTests(unittest.TestCase):
    def test_drops_oldest_turns_before_sending_approximately_4k_tokens(self):
        model = object.__new__(LLMModel)
        model.system_message = {"role": "system", "content": "Сценарий " * 200}
        model.dialog_history = [
            {"role": "user" if index % 2 == 0 else "assistant", "content": f"Реплика {index}: " + "слово " * 170}
            for index in range(12)
        ]
        current = {"role": "user", "content": "Что случилось?"}

        messages = model._request_messages(current)

        self.assertEqual(model.system_message, messages[0])
        self.assertEqual(current, messages[-1])
        self.assertLess(len(model.dialog_history), 12)
        self.assertEqual(messages[1:-1], model.dialog_history)
        self.assertLessEqual(sum(map(model._estimate_tokens, messages)), model.MAX_INPUT_TOKENS)

    def test_caller_opening_excludes_hidden_facts_only_for_first_reply(self):
        model = object.__new__(LLMModel)
        model.system_message = {"role": "system", "content": "Факт: дышит тяжело"}
        model.opening_system_message = {"role": "system", "content": "Факт ещё неизвестен"}
        model.dialog_history = []

        self.assertEqual("Факт ещё неизвестен", model._request_messages({"role": "user", "content": "Что случилось?"})[0]["content"])
        model.dialog_history.append({"role": "assistant", "content": "Боль в груди"})
        self.assertEqual("Факт: дышит тяжело", model._request_messages({"role": "user", "content": "Он дышит?"})[0]["content"])

    def test_preserves_system_prompt_even_when_it_exceeds_budget(self):
        model = object.__new__(LLMModel)
        model.system_message = {"role": "system", "content": "факт " * 3000}
        model.dialog_history = [{"role": "assistant", "content": "старый ответ"}]

        with self.assertLogs(level="WARNING"):
            messages = model._request_messages({"role": "user", "content": "Вопрос"})

        self.assertEqual([], model.dialog_history)
        self.assertEqual(model.system_message, messages[0])


if __name__ == "__main__":
    unittest.main()
