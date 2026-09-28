from dataclasses import dataclass
from enum import Enum


class DialogStatus(Enum):
    IDLE = "IDLE"
    IN_CALL = "IN_CALL"
    DISCONNECTED = "DISCONNECTED"
    COMPLETED = "COMPLETED"


class Speaker(Enum):
    OPERATOR = "OPERATOR"
    COUNTERPARTY = "COUNTERPARTY"


class CallDirection(Enum):
    INBOUND = "INBOUND"
    OUTBOUND = "OUTBOUND"
    UNSPECIFIED = "UNSPECIFIED"


class CounterpartyType(Enum):
    CALLER = "CALLER"
    BRIGADE = "BRIGADE"
    SERVICE = "SERVICE"
    UNSPECIFIED = "UNSPECIFIED"


class Gender(Enum):
    MAN = "MAN"
    WOMEN = "WOMEN"
    UNSPECIFIED = "UNSPECIFIED"


@dataclass(frozen=True)
class Person:
    first_name: str = ""
    last_name: str = ""
    middle_name: str = ""
    age: int | None = None
    phone: str = ""
    contact_phone: str = ""
    address: str = ""
    additional_info: str = ""


@dataclass(frozen=True)
class CallScenario:
    id: str
    position: int
    direction: CallDirection
    counterparty: CounterpartyType
    person: Person
    gender: Gender
    known_facts: tuple[str, ...]
    hidden_facts: tuple[str, ...]
    ai_context: str
    emotional_state: str


@dataclass(frozen=True)
class DialogProgress:
    context_id: str
    active_call_id: str
    status: DialogStatus


@dataclass(frozen=True)
class Phrase:
    speaker: Speaker
    text: str


@dataclass(frozen=True)
class DialogTranscript:
    phrases: tuple[Phrase, ...]
