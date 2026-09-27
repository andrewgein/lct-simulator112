from os import getenv
import threading
import time
import regex
from enum import Enum
from openai import OpenAI
import logging
import asyncio

from app.grpc.com.simulator112.incident.incident_context_pb2 import DialupContext
from app.prompts import CALLER_SYSTEM_PROMPT, build_dialup_scenario
from app.utils.llm_model import LLMModel
from .processing_node import UserDialogProcessingNode

logger = logging.getLogger()


DELIMITERS_SEARCH_PATTERN = r'([!?]+|(?:(?<!\d)\.|(?<=\d)\.)(?!\d)(?:\.(?!\d))*)'


class ChatNode(UserDialogProcessingNode):
    worker: asyncio.Task | None
    def __init__(self, context: DialupContext, on_new_phrase=lambda text: None):
        super().__init__()
        self.context = context
        self.on_new_phrase = on_new_phrase
        self.previous_event = ""
        self.pending_text = ""
        self.response_buffer = []
        self.partial_response = ""

        self.worker = None
        self.loop = asyncio.new_event_loop()
        self.loop_thread = threading.Thread(target=self._run_event_loop, daemon=True)
        self.loop_thread.start()

        system_prompt = getenv("LLM_SYSTEM_PROMPT", CALLER_SYSTEM_PROMPT)
        incident_scenario = build_dialup_scenario(context)
        full_prompt = f"{system_prompt}\n\n{incident_scenario}"
        self.model = LLMModel(full_prompt)


    def _run_event_loop(self):
        asyncio.set_event_loop(self.loop)
        self.loop.run_forever()


    def _event_handler(self, event):
        if self.worker is not None and not self.worker.done():
            future = asyncio.run_coroutine_threadsafe(
                self._restart_worker(self.previous_event, event),
                self.loop,
            )
            self.worker = future.result()
        else:
            future = asyncio.run_coroutine_threadsafe(
                    self._start_worker(event),
                    self.loop,
                )
            self.worker = future.result()
        self.previous_event = event

    async def _start_worker(self, event: str) -> asyncio.Task:
        return asyncio.create_task(self._llm_worker(event))

    async def _restart_worker(self, old_user_text: str, new_user_text: str) -> asyncio.Task:
        old_worker = self.worker
        if old_worker is not None and not old_worker.done():
            old_worker.cancel()
            already_generated_response_buffer = await old_worker
            complted_sentences = self._get_complete_sentences(already_generated_response_buffer)
            # TODO: if no sentence is generated -> Охлади свое траханье!
            return asyncio.create_task(self._llm_worker(new_user_text, old_user_text, complted_sentences))
        return asyncio.create_task(self._llm_worker(new_user_text))

    def _get_complete_sentences(self, text: str) -> str:
        delimiter_pos = [d.start() for d in regex.finditer(DELIMITERS_SEARCH_PATTERN, text)]
        if not delimiter_pos:
            return ""

        last_delimiter_pos = delimiter_pos[-1]
        return text[:last_delimiter_pos + 1]


    async def _llm_worker(self,
                          user_text: str,
                          previous_user_text: str | None = None,
                          previous_response: str | None = None):
        generator = None
        full_response_buffer = []
        self._reset_buffers()
        try:
            if (previous_user_text is not None and previous_response is not None):
                generator = self.model.regenerate_answer(user_text, previous_user_text, previous_response)
            else:
                generator = self.model.generate_answer(user_text)

            async for chunk in generator:

                clean_text = chunk
                if (clean_text != ""):
                    logger.info("New LLM response chunk: " + str(clean_text))
                    self._append_to_buffer(clean_text)
                    full_response_buffer.append(clean_text)

            if self.pending_text.strip():
                self.response_buffer.append(self.pending_text)
                self.pending_text = ""
                self._flush_buffer()
            full_response = " ".join(full_response_buffer)
            self.on_new_phrase(full_response)
        except asyncio.CancelledError:
            logger.info("LLM was interrupted by user")
            if generator is not None:
                await generator.aclose()
            return "".join(full_response_buffer)

    def _reset_buffers(self):
        self.response_buffer = []
        self.pending_text = ""

    def _flush_buffer(self):
        sentence = "".join(self.response_buffer)
        logger.info("Flushing LLM response buffer: " + sentence)
        self.output_queue.put(sentence)
        self.response_buffer = []

    def _append_to_buffer(self, text):
        ext = self.pending_text + text
        self.pending_text = ""

        parts = regex.split(DELIMITERS_SEARCH_PATTERN, ext)

        for i in range(0, len(parts) - 1, 2):
            full_sentence = parts[i] + parts[i+1]

            if full_sentence.strip():
                self.response_buffer.append(full_sentence)
                self._flush_buffer()

        leftover = parts[-1]
        if leftover:
            self.pending_text = leftover
