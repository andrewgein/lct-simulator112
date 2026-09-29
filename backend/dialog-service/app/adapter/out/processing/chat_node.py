from os import getenv
import threading
import time
import regex
from enum import Enum
from openai import OpenAI
import logging
import asyncio

from app.domain.model import CallDirection, CallScenario, CounterpartyType, DialogTranscript, Speaker
from app.application.model.prompts import BRIGADE_INBOUND_SYSTEM_PROMPT, BRIGADE_OUTBOUND_SYSTEM_PROMPT, CALLER_SYSTEM_PROMPT, SERVICE_SYSTEM_PROMPT, build_call_scenario
from app.adapter.out.processing.llm_model import LLMModel
from .processing_node import UserDialogProcessingNode

logger = logging.getLogger()

DELIMITERS_SEARCH_PATTERN = r'([!?]+|(?<!\d)\.+|\.(?!\d))'


def _preprocess_text(text: str) -> str:
    # Keep digits and context until TTSTextPreprocessor, immediately before stress.
    text = regex.sub(r'[\u2010-\u2015\u2212]', '-', text)

    return text


class ChatNode(UserDialogProcessingNode):
    worker: asyncio.Task | None
    def __init__(self, context: CallScenario, on_new_phrase=lambda text: None,
                 history: DialogTranscript | None = None):
        super().__init__()
        self.context = context
        self.on_new_phrase = on_new_phrase
        self.previous_event = ""
        self.pending_text = ""
        self.response_buffer = []
        self.partial_response = ""

        self.worker = None
        self._opening_started = False
        self.loop = asyncio.new_event_loop()
        self.loop_thread = threading.Thread(target=self._run_event_loop, daemon=True)
        self.loop_thread.start()

        default_prompt = {
            CounterpartyType.BRIGADE: (BRIGADE_INBOUND_SYSTEM_PROMPT if context.direction == CallDirection.INBOUND
                                       else BRIGADE_OUTBOUND_SYSTEM_PROMPT),
            CounterpartyType.SERVICE: SERVICE_SYSTEM_PROMPT,
        }.get(context.counterparty, CALLER_SYSTEM_PROMPT)
        system_prompt = getenv("LLM_SYSTEM_PROMPT", default_prompt)
        incident_scenario = build_call_scenario(context)
        full_prompt = f"{system_prompt}\n\n{incident_scenario}"
        self.model = LLMModel(full_prompt)
        if context.counterparty == CounterpartyType.CALLER:
            self.model.opening_system_message = {
                "role": "system",
                "content": f"{system_prompt}\n\n{build_call_scenario(context, include_hidden=False)}",
            }
        if history is not None and history.phrases:
            roles = {Speaker.OPERATOR: "user", Speaker.COUNTERPARTY: "assistant"}
            self.model.dialog_history = [
                {"role": roles[phrase.speaker], "content": phrase.text}
                for phrase in history.phrases
            ][-10:]


    def begin_call(self) -> None:
        if (not self._opening_started and self.context.direction == CallDirection.INBOUND
                and self.context.counterparty in (CounterpartyType.BRIGADE, CounterpartyType.SERVICE)
                and not self.model.dialog_history):
            self._opening_started = True
            self.input_queue.put(None)

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

    async def _start_worker(self, event: str | None) -> asyncio.Task:
        return asyncio.create_task(self._llm_worker(event))

    async def _restart_worker(self, old_user_text: str | None, new_user_text: str) -> asyncio.Task:
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
                          user_text: str | None,
                          previous_user_text: str | None = None,
                          previous_response: str | None = None):
        generator = None
        self._reset_buffers()
        try:
            if previous_response is not None:
                generator = self.model.regenerate_answer(user_text, previous_user_text, previous_response)
            else:
                generator = self.model.generate_answer(user_text)

            async for chunk in generator:
                if chunk:
                    logger.info("New LLM response chunk: " + str(chunk))
                    self._append_to_buffer(chunk)
            self._flush_pending()
            self.on_new_phrase(self._processed_response())
        except asyncio.CancelledError:
            logger.info("LLM was interrupted by user")
            if generator is not None:
                await generator.aclose()
            return self._processed_response()

    def _reset_buffers(self):
        self.response_buffer = []
        self.pending_text = ""
        self.completed_sentences = []

    def _processed_response(self):
        tail = "".join(self.response_buffer) + self.pending_text
        return "".join(self.completed_sentences) + (_preprocess_text(tail) if tail else "")

    def _flush_buffer(self):
        sentence = _preprocess_text("".join(self.response_buffer))
        logger.info("Flushing LLM response buffer: " + sentence)
        self.output_queue.put(sentence)
        self.completed_sentences.append(sentence)
        self.response_buffer = []

    def _flush_pending(self):
        if self.pending_text.strip():
            self.response_buffer.append(self.pending_text)
            self.pending_text = ""
            self._flush_buffer()

    def _append_to_buffer(self, text):
        ext = self.pending_text + text
        self.pending_text = ""

        start = 0
        for delimiter in regex.finditer(DELIMITERS_SEARCH_PATTERN, ext):
            # A trailing dot after a digit may belong to a decimal in the next chunk.
            if (delimiter.group() == "." and delimiter.end() == len(ext)
                    and delimiter.start() > 0 and ext[delimiter.start() - 1].isdigit()):
                break
            full_sentence = ext[start:delimiter.end()]
            start = delimiter.end()

            if full_sentence.strip():
                self.response_buffer.append(full_sentence)
                self._flush_buffer()

        leftover = ext[start:]
        if leftover:
            self.pending_text = leftover
