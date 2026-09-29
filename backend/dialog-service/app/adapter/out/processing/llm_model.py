import logging
import time
from os import getenv
from typing import Any, AsyncGenerator, Generator
from openai import AsyncOpenAI, Stream
from openai.types.chat import ChatCompletionChunk
from app.adapter.out.processing.latency_tracker import tracker

logger = logging.getLogger()

class LLMModel:
    # Conservative estimate for Cyrillic without a tokenizer for the configured provider.
    # Leave room in the ~4k-token window for the model's reply.
    MAX_INPUT_TOKENS = 3700

    @staticmethod
    def _estimate_tokens(message: dict) -> int:
        return (len(message["content"].encode("utf-8")) + 1) // 2 + 12

    def _request_messages(self, user_message: dict) -> list[dict]:
        opening = getattr(self, "opening_system_message", None)
        system = (opening if opening is not None and not self.dialog_history
                  and not getattr(self, "_opening_completed", False) else self.system_message)
        messages = [system, *self.dialog_history, user_message]
        while self.dialog_history and sum(map(self._estimate_tokens, messages)) > self.MAX_INPUT_TOKENS:
            self.dialog_history.pop(0)
            # Once a conversation has started, never return to the opening prompt.
            messages = [self.system_message, *self.dialog_history, user_message]
        if sum(map(self._estimate_tokens, messages)) > self.MAX_INPUT_TOKENS:
            logger.warning("System prompt and current message exceed the approximate 4k-token input budget")
        return messages

    def __init__(self, system_prompt: str):
        self.model = getenv("LLM_MODEL", "gpt-oss-120b")
        model_base_url = getenv("LLM_BASE_URL", "https://api.aitunnel.ru/v1/")
        model_api_key = getenv("LLM_API_KEY")
        self.client = AsyncOpenAI(base_url=model_base_url, api_key=model_api_key)
        self.dialog_history = []
        self.system_message = {
            "role": "system",
            "content": system_prompt
        }

    async def _stream_and_save(self, stream, user_text: str | None, request_started: float, assistant_prefix: str = "") -> AsyncGenerator[str, Any]:
        collected_chunks = []
        first_chunk_seen = False
        try:
            async for chunk in stream:
                content = chunk.choices[0].delta.content if chunk.choices else None
                if content is not None:
                    if not first_chunk_seen:
                        first_chunk_seen = True
                        tracker.record("llm", time.monotonic() - request_started)
                    collected_chunks.append(content)
                    yield content
        except (GeneratorExit, Exception):
            raise
        else:
            full_text = "".join(collected_chunks)
            if full_text.strip():
                if user_text is not None:
                    self.dialog_history.append({"role": "user", "content": user_text})
                self.dialog_history.append({"role": "assistant", "content": full_text})
                self._opening_completed = True
                self.dialog_history = self.dialog_history[-10:]


    async def generate_answer(self, user_text) -> AsyncGenerator[str, Any]:
        user_message = {"role": "user", "content": user_text if user_text is not None else
                        "Соединение установлено. Это служебный сигнал, не реплика оператора. "
                        "Произнеси первую реплику входящего звонка по разделу «НАЧАЛО РАЗГОВОРА»."}
        request_messages = self._request_messages(user_message)
        request_started = time.monotonic()
        stream = await self.client.chat.completions.create(
                model=self.model,
                messages=request_messages,
                stream=True,
                extra_body={
                    "reasoning": {"effort": "low", "exclude": True},
                    "provider": {"sort": "price"}
                }
            )
        async for chunk in self._stream_and_save(stream, user_text, request_started):
            yield chunk


    async def regenerate_answer(self, new_user_text: str, previous_user_text: str | None, partial_response: str) -> AsyncGenerator[str, Any]:

        logger.info(f"Regenerating new_user_text='{new_user_text}' previous_user_text='{previous_user_text}' partial_response='{partial_response}'")
        if previous_user_text is not None:
            self.dialog_history.append({"role": "user", "content": previous_user_text})

        if partial_response.strip():
            self.dialog_history.append({"role": "assistant", "content": partial_response})

        self.dialog_history = self.dialog_history[-10:]
        request_messages = self._request_messages({"role": "user", "content": new_user_text})

        request_started = time.monotonic()
        stream = await self.client.chat.completions.create(
            model=self.model,
            messages=request_messages,
            stream=True,
            extra_body={
                "reasoning": {"effort": "low", "exclude": True},
                "provider": {"sort": "price"}
            }
        )

        async for chunk in self._stream_and_save(stream, new_user_text, request_started, assistant_prefix=partial_response):
            yield chunk
