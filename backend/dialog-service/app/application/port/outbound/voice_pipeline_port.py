from collections.abc import Callable
from typing import Protocol

from app.domain.model import CallScenario


class VoicePipeline(Protocol):
    def process_text(self, text: str) -> None: ...
    def process_audio(self, pcm: bytes) -> None: ...
    def close(self) -> None: ...


class VoicePipelineFactory(Protocol):
    def create(
        self,
        call: CallScenario,
        on_operator_phrase: Callable[[str], None],
        on_counterparty_phrase: Callable[[str], None],
        on_audio: Callable[[bytes], None],
    ) -> VoicePipeline: ...

    def warm_up(self) -> None: ...
