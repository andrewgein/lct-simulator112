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
        self.assertEqual(sentences, ["Нужно двенадцать человек."])
        self.assertEqual(phrases, ["Нужно двенадцать человек."])
        self.assertEqual(preprocess.call_count, 1)

    async def test_time_split_across_chunks(self):
        sentences, phrases = await self._run_chunks(["Встреча в ", "12:", "30."])
        self.assertEqual(sentences, ["Встреча в двенадцать тридцать."])
        self.assertEqual(phrases, ["Встреча в двенадцать тридцать."])

    async def test_unfinished_sentence_normalized_for_callback(self):
        sentences, phrases = await self._run_chunks(["У нас ", "1", "2"])
        self.assertEqual(sentences, [])
        self.assertEqual(phrases, ["У нас двенадцать"])

    async def test_number_split_between_sentences(self):
        sentences, phrases = await self._run_chunks(["Пришёл 1", "2. Затем ", "3", " человека."])
        self.assertEqual(sentences, ["Пришёл двенадцать.", " Затем три человека."])
        self.assertEqual(phrases, ["Пришёл двенадцать. Затем три человека."])
