from collections.abc import Callable

import numpy as np

from app.adapter.out.processing.chat_node import ChatNode
from app.adapter.out.processing.output_node import OutputNode
from app.adapter.out.processing.processing_context import UserDialogProcessingContext
from app.adapter.out.processing.sst_node import SSTNode
from app.adapter.out.processing.tts_model import TTSModel
from app.adapter.out.processing.tts_node import TTSNode
from app.application.port.outbound import VoicePipeline, VoicePipelineFactory
from app.domain.model import CallScenario


class ProcessingVoicePipeline(VoicePipeline):
    def __init__(
        self,
        call: CallScenario,
        on_operator_phrase: Callable[[str], None],
        on_counterparty_phrase: Callable[[str], None],
        on_audio: Callable[[bytes], None],
    ):
        def output_callback(audio_chunk) -> None:
            audio = np.asarray(audio_chunk, dtype=np.float32) * 32767.0
            np.clip(audio, -32768, 32767, out=audio)
            on_audio(audio.astype(np.int16).tobytes())

        processing_context = UserDialogProcessingContext()
        sst = SSTNode(on_new_phrase=on_operator_phrase)
        chat = ChatNode(context=call, on_new_phrase=on_counterparty_phrase)
        tts = TTSNode(call)
        output = OutputNode(output_callback)

        (processing_context
            .connect(sst)
            .connect(chat)
            .connect(tts)
            .connect(output))

        self._context = processing_context

    def process_text(self, text: str) -> None:
        self._context.process(text)

    def process_audio(self, pcm: bytes) -> None:
        self._context.process(np.frombuffer(pcm, dtype=np.int16))

    def close(self) -> None:
        self._context.close()


class ProcessingVoicePipelineFactory(VoicePipelineFactory):
    def create(
        self,
        call: CallScenario,
        on_operator_phrase: Callable[[str], None],
        on_counterparty_phrase: Callable[[str], None],
        on_audio: Callable[[bytes], None],
    ) -> VoicePipeline:
        return ProcessingVoicePipeline(
            call, on_operator_phrase, on_counterparty_phrase, on_audio)

    def warm_up(self) -> None:
        TTSModel()
