from threading import Lock

from app.domain.model import DialogTranscript, Phrase, Speaker


class DialogContextBuilder:
    def __init__(self):
        self._phrases: list[Phrase] = []
        self._lock = Lock()

    def append_user_phrase(self, text: str) -> None:
        with self._lock:
            self._phrases.append(Phrase(speaker=Speaker.OPERATOR, text=text))

    def append_llm_phrase(self, text: str) -> None:
        with self._lock:
            self._phrases.append(Phrase(speaker=Speaker.COUNTERPARTY, text=text))

    def get(self) -> DialogTranscript:
        with self._lock:
            return DialogTranscript(tuple(self._phrases))
