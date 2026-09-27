from queue import Queue
import unittest
from unittest.mock import patch

from app.adapter.out.processing.chat_node import ChatNode, _preprocess_text


class _StreamingModel:
    def __init__(self, chunks):
        self.chunks = chunks

    async def generate_answer(self, _user_text):
        for chunk in self.chunks:
            yield chunk


class _TestChatNode(ChatNode):
    def __del__(self):
        pass  # No worker thread was started in these tests.


class ChatNodePreprocessingTests(unittest.IsolatedAsyncioTestCase):
    async def _run_chunks(self, chunks):
        node = object.__new__(_TestChatNode)
        node.output_queue = Queue()
        node.response_buffer = []
        node.pending_text = ""
        node.model = _StreamingModel(chunks)
        phrases = []
        node.on_new_phrase = phrases.append
        await node._llm_worker("question")
        return list(node.output_queue.queue), phrases

    async def test_number_split_across_chunks(self):
        with patch("app.adapter.out.processing.chat_node._preprocess_text", wraps=_preprocess_text) as preprocess:
            sentences, phrases = await self._run_chunks(["Нужно ", "1", "2", " человек."])
        self.assertEqual(sentences, ["Нужно 12 человек."])
        self.assertEqual(phrases, ["Нужно 12 человек."])
        self.assertEqual(preprocess.call_count, 1)

    async def test_time_split_across_chunks(self):
        sentences, phrases = await self._run_chunks(["Встреча в ", "12:", "30."])
        self.assertEqual(sentences, ["Встреча в 12:30."])
        self.assertEqual(phrases, ["Встреча в 12:30."])

    async def test_unfinished_sentence_still_reaches_tts(self):
        sentences, phrases = await self._run_chunks(["У нас ", "1", "2"])
        self.assertEqual(sentences, ["У нас 12"])
        self.assertEqual(phrases, ["У нас 12"])

    async def test_number_split_between_sentences(self):
        sentences, phrases = await self._run_chunks(["Пришёл 1", "2. Затем ", "3", " человека."])
        self.assertEqual(sentences, ["Пришёл 12.", " Затем 3 человека."])
        self.assertEqual(phrases, ["Пришёл 12. Затем 3 человека."])

    async def test_decimal_split_after_dot(self):
        sentences, phrases = await self._run_chunks(["Температура 38.", "5. На 3 этаже."])
        self.assertEqual(sentences, ["Температура 38.5.", " На 3 этаже."])
        self.assertEqual(phrases, ["Температура 38.5. На 3 этаже."])

    async def test_final_dot_after_number_is_flushed(self):
        sentences, phrases = await self._run_chunks(["Номер 12."])
        self.assertEqual(sentences, ["Номер 12."])
        self.assertEqual(phrases, ["Номер 12."])

    async def test_stream_to_number_normalizer(self):
        from app.adapter.out.processing.tts_text_preprocessor import normalize_numbers
        sentences, _ = await self._run_chunks(["На ", "3 этаже 2 маш", "ины."])
        self.assertEqual([normalize_numbers(s) for s in sentences], ["На третьем этаже две машины."])
