from threading import Lock

from app.application.port.outbound import ContextPort, NoMoreCallsError
from app.domain.model import (
    CallDirection, CallScenario, CounterpartyType, DialogProgress, DialogStatus,
    DialogTranscript, Gender, Person,
)


class MockContextAdapter(ContextPort):
    def __init__(self):
        self._lock = Lock()
        self._progress: dict[str, DialogProgress] = {}
        self._transcripts: dict[str, list] = {}
        self._calls = (_mock_call(),)

    def get_progress(self, context_id: str) -> DialogProgress:
        with self._lock:
            return self._progress.get(context_id, DialogProgress(context_id, "", DialogStatus.IDLE))

    def get_next_call(self, context_id: str, current_call_id: str = "-1") -> CallScenario:
        ids = [call.id for call in self._calls]
        index = ids.index(current_call_id) + 1 if current_call_id in ids else 0
        if index >= len(self._calls):
            raise NoMoreCallsError("Доступные звонки закончились")
        return self._calls[index]

    def get_call(self, context_id: str, call_id: str) -> CallScenario:
        for call in self._calls:
            if call.id == call_id:
                return call
        raise ValueError("Звонок не найден")

    def start_call(self, context_id: str, call_id: str) -> DialogProgress:
        self.get_call(context_id, call_id)
        progress = DialogProgress(context_id, call_id, DialogStatus.IN_CALL)
        self._progress[context_id] = progress
        return progress

    def complete_call(self, context_id: str, call_id: str) -> DialogProgress:
        return self._change(context_id, call_id, DialogStatus.COMPLETED)

    def disconnect_call(self, context_id: str, call_id: str) -> DialogProgress:
        return self._change(context_id, call_id, DialogStatus.DISCONNECTED)

    def append_transcript(self, context_id: str, transcript: DialogTranscript) -> None:
        self._transcripts.setdefault(context_id, []).extend(transcript.phrases)

    def _change(self, context_id: str, call_id: str, status: DialogStatus) -> DialogProgress:
        current = self.get_progress(context_id)
        if current.active_call_id != call_id:
            raise ValueError("Звонок не активен")
        progress = DialogProgress(context_id, call_id, status)
        self._progress[context_id] = progress
        return progress


def _mock_call() -> CallScenario:
    return CallScenario(
        id="mock-call-1", position=1, direction=CallDirection.INBOUND,
        counterparty=CounterpartyType.CALLER,
        person=Person(first_name="София", last_name="Иванова", age=8,
                      phone="8 (961) 263-36-64",
                      address="Москва, улица Академика Королёва, дом 14, квартира 87"),
        gender=Gender.WOMEN,
        emotional_state="PANICKED",
        known_facts=("Мама внезапно упала и не отвечает", "Девочка находится дома вместе с мамой"),
        hidden_facts=("Неизвестно, дышит ли мама", "Рядом нет другого взрослого"),
        ai_context="Вы — маленькая девочка София. Отвечайте коротко и не подсказывайте оператору.",
    )
