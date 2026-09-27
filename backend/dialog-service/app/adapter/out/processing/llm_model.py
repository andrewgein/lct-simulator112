import logging
from os import getenv
from typing import Any, AsyncGenerator, Generator
from openai import AsyncOpenAI, Stream
from openai.types.chat import ChatCompletionChunk

logger = logging.getLogger()

class LLMModel:
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

    async def _stream_and_save(self, stream, user_text: str) -> AsyncGenerator[str, Any]:
        collected_chunks = []
        try:
            async for chunk in stream:
                content = chunk.choices[0].delta.content if chunk.choices else None
                if content is not None:
                    collected_chunks.append(content)
                    yield content
        except (GeneratorExit, Exception):
            raise
        else:
            full_text = "".join(collected_chunks)
            if full_text.strip():
                self.dialog_history.append({"role": "user", "content": user_text})
                self.dialog_history.append({"role": "assistant", "content": full_text})
                self.dialog_history = self.dialog_history[-10:]


    async def generate_answer(self, user_text) -> AsyncGenerator[str, Any]:
        user_message = {"role": "user", "content": user_text}
        request_messages = [self.system_message, *self.dialog_history, user_message]
        stream = await self.client.chat.completions.create(
                model=self.model,
                messages=request_messages,
                stream=True,
                extra_body={
                    "reasoning": {"effort": "low", "exclude": True},
                    "provider": {"sort": "price"}
                }
            )
        async for chunk in self._stream_and_save(stream, user_text):
            yield chunk


    async def regenerate_answer(self, new_user_text: str, previous_user_text: str, partial_response: str) -> AsyncGenerator[str, Any]:

        logger.info(f"Regenerating new_user_text='{new_user_text}' previous_user_text='{previous_user_text}' partial_response='{partial_response}'")
        self.dialog_history.append({"role": "user", "content": previous_user_text})

        if partial_response.strip():
            self.dialog_history.append({"role": "assistant", "content": partial_response})

        self.dialog_history = self.dialog_history[-10:]
        request_messages = [self.system_message, *self.dialog_history, {"role": "user", "content": new_user_text}]

        stream = await self.client.chat.completions.create(
            model=self.model,
            messages=request_messages,
            stream=True,
            extra_body={
                "reasoning": {"effort": "low", "exclude": True},
                "provider": {"sort": "price"}
            }
        )

        async for chunk in self._stream_and_save(stream, new_user_text):
            yield chunk
