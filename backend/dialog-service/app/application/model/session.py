from dataclasses import dataclass

from app.domain.model import CallScenario, DialogProgress


@dataclass(frozen=True)
class DialogSession:
    progress: DialogProgress
    call: CallScenario | None
