from google.protobuf import empty_pb2 as _empty_pb2
from app.grpc.com.simulator112.incident import incident_context_pb2 as _incident_context_pb2
from google.protobuf.internal import containers as _containers
from google.protobuf.internal import enum_type_wrapper as _enum_type_wrapper
from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from collections.abc import Iterable as _Iterable, Mapping as _Mapping
from typing import ClassVar as _ClassVar, Optional as _Optional, Union as _Union

DESCRIPTOR: _descriptor.FileDescriptor

class DialogProgressStatus(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    DIALOG_PROGRESS_STATUS_UNSPECIFIED: _ClassVar[DialogProgressStatus]
    IDLE: _ClassVar[DialogProgressStatus]
    IN_CALL: _ClassVar[DialogProgressStatus]
    DISCONNECTED: _ClassVar[DialogProgressStatus]
    COMPLETED: _ClassVar[DialogProgressStatus]

class SpeakerType(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    USER: _ClassVar[SpeakerType]
    LLM: _ClassVar[SpeakerType]

class SolutionContextStatus(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    SOLUTION_CONTEXT_STATUS_UNSPECIFIED: _ClassVar[SolutionContextStatus]
    ACTIVE: _ClassVar[SolutionContextStatus]
    CLOSED_DUPLICATE: _ClassVar[SolutionContextStatus]
DIALOG_PROGRESS_STATUS_UNSPECIFIED: DialogProgressStatus
IDLE: DialogProgressStatus
IN_CALL: DialogProgressStatus
DISCONNECTED: DialogProgressStatus
COMPLETED: DialogProgressStatus
USER: SpeakerType
LLM: SpeakerType
SOLUTION_CONTEXT_STATUS_UNSPECIFIED: SolutionContextStatus
ACTIVE: SolutionContextStatus
CLOSED_DUPLICATE: SolutionContextStatus

class PersonInfo(_message.Message):
    __slots__ = ("phone", "contact_phone", "last_name", "first_name", "middle_name", "address", "additional_info")
    PHONE_FIELD_NUMBER: _ClassVar[int]
    CONTACT_PHONE_FIELD_NUMBER: _ClassVar[int]
    LAST_NAME_FIELD_NUMBER: _ClassVar[int]
    FIRST_NAME_FIELD_NUMBER: _ClassVar[int]
    MIDDLE_NAME_FIELD_NUMBER: _ClassVar[int]
    ADDRESS_FIELD_NUMBER: _ClassVar[int]
    ADDITIONAL_INFO_FIELD_NUMBER: _ClassVar[int]
    phone: str
    contact_phone: str
    last_name: str
    first_name: str
    middle_name: str
    address: str
    additional_info: str
    def __init__(self, phone: _Optional[str] = ..., contact_phone: _Optional[str] = ..., last_name: _Optional[str] = ..., first_name: _Optional[str] = ..., middle_name: _Optional[str] = ..., address: _Optional[str] = ..., additional_info: _Optional[str] = ...) -> None: ...

class DialogProgress(_message.Message):
    __slots__ = ("context_id", "active_dialup_id", "status")
    CONTEXT_ID_FIELD_NUMBER: _ClassVar[int]
    ACTIVE_DIALUP_ID_FIELD_NUMBER: _ClassVar[int]
    STATUS_FIELD_NUMBER: _ClassVar[int]
    context_id: str
    active_dialup_id: str
    status: DialogProgressStatus
    def __init__(self, context_id: _Optional[str] = ..., active_dialup_id: _Optional[str] = ..., status: _Optional[_Union[DialogProgressStatus, str]] = ...) -> None: ...

class Phrase(_message.Message):
    __slots__ = ("speaker", "text")
    SPEAKER_FIELD_NUMBER: _ClassVar[int]
    TEXT_FIELD_NUMBER: _ClassVar[int]
    speaker: SpeakerType
    text: str
    def __init__(self, speaker: _Optional[_Union[SpeakerType, str]] = ..., text: _Optional[str] = ...) -> None: ...

class DialogContext(_message.Message):
    __slots__ = ("transcript",)
    TRANSCRIPT_FIELD_NUMBER: _ClassVar[int]
    transcript: _containers.RepeatedCompositeFieldContainer[Phrase]
    def __init__(self, transcript: _Optional[_Iterable[_Union[Phrase, _Mapping]]] = ...) -> None: ...

class SolutionContext(_message.Message):
    __slots__ = ("applicant", "victim", "additional_info", "incident_type", "dialup_id", "parent_id", "duplicate_of_id", "revision_id", "card_id", "previous_revision_id", "version", "status", "created_at", "additional_info_provided")
    class AdditionalInfoEntry(_message.Message):
        __slots__ = ("key", "value")
        KEY_FIELD_NUMBER: _ClassVar[int]
        VALUE_FIELD_NUMBER: _ClassVar[int]
        key: str
        value: str
        def __init__(self, key: _Optional[str] = ..., value: _Optional[str] = ...) -> None: ...
    APPLICANT_FIELD_NUMBER: _ClassVar[int]
    VICTIM_FIELD_NUMBER: _ClassVar[int]
    ADDITIONAL_INFO_FIELD_NUMBER: _ClassVar[int]
    INCIDENT_TYPE_FIELD_NUMBER: _ClassVar[int]
    DIALUP_ID_FIELD_NUMBER: _ClassVar[int]
    PARENT_ID_FIELD_NUMBER: _ClassVar[int]
    DUPLICATE_OF_ID_FIELD_NUMBER: _ClassVar[int]
    REVISION_ID_FIELD_NUMBER: _ClassVar[int]
    CARD_ID_FIELD_NUMBER: _ClassVar[int]
    PREVIOUS_REVISION_ID_FIELD_NUMBER: _ClassVar[int]
    VERSION_FIELD_NUMBER: _ClassVar[int]
    STATUS_FIELD_NUMBER: _ClassVar[int]
    CREATED_AT_FIELD_NUMBER: _ClassVar[int]
    ADDITIONAL_INFO_PROVIDED_FIELD_NUMBER: _ClassVar[int]
    applicant: PersonInfo
    victim: PersonInfo
    additional_info: _containers.ScalarMap[str, str]
    incident_type: str
    dialup_id: str
    parent_id: str
    duplicate_of_id: str
    revision_id: str
    card_id: str
    previous_revision_id: str
    version: int
    status: SolutionContextStatus
    created_at: str
    additional_info_provided: bool
    def __init__(self, applicant: _Optional[_Union[PersonInfo, _Mapping]] = ..., victim: _Optional[_Union[PersonInfo, _Mapping]] = ..., additional_info: _Optional[_Mapping[str, str]] = ..., incident_type: _Optional[str] = ..., dialup_id: _Optional[str] = ..., parent_id: _Optional[str] = ..., duplicate_of_id: _Optional[str] = ..., revision_id: _Optional[str] = ..., card_id: _Optional[str] = ..., previous_revision_id: _Optional[str] = ..., version: _Optional[int] = ..., status: _Optional[_Union[SolutionContextStatus, str]] = ..., created_at: _Optional[str] = ..., additional_info_provided: _Optional[bool] = ...) -> None: ...

class FullContext(_message.Message):
    __slots__ = ("uuid", "user_id", "incident_context", "dialog_context", "solution_context", "level_context", "solution_context_revisions")
    UUID_FIELD_NUMBER: _ClassVar[int]
    USER_ID_FIELD_NUMBER: _ClassVar[int]
    INCIDENT_CONTEXT_FIELD_NUMBER: _ClassVar[int]
    DIALOG_CONTEXT_FIELD_NUMBER: _ClassVar[int]
    SOLUTION_CONTEXT_FIELD_NUMBER: _ClassVar[int]
    LEVEL_CONTEXT_FIELD_NUMBER: _ClassVar[int]
    SOLUTION_CONTEXT_REVISIONS_FIELD_NUMBER: _ClassVar[int]
    uuid: str
    user_id: str
    incident_context: _incident_context_pb2.IncidentContext
    dialog_context: DialogContext
    solution_context: SolutionContext
    level_context: _incident_context_pb2.LevelContext
    solution_context_revisions: _containers.RepeatedCompositeFieldContainer[SolutionContext]
    def __init__(self, uuid: _Optional[str] = ..., user_id: _Optional[str] = ..., incident_context: _Optional[_Union[_incident_context_pb2.IncidentContext, _Mapping]] = ..., dialog_context: _Optional[_Union[DialogContext, _Mapping]] = ..., solution_context: _Optional[_Union[SolutionContext, _Mapping]] = ..., level_context: _Optional[_Union[_incident_context_pb2.LevelContext, _Mapping]] = ..., solution_context_revisions: _Optional[_Iterable[_Union[SolutionContext, _Mapping]]] = ...) -> None: ...

class GetIncidentContextRequest(_message.Message):
    __slots__ = ("uuid",)
    UUID_FIELD_NUMBER: _ClassVar[int]
    uuid: str
    def __init__(self, uuid: _Optional[str] = ...) -> None: ...

class AppendDialogContextRequest(_message.Message):
    __slots__ = ("uuid", "dialog_context")
    UUID_FIELD_NUMBER: _ClassVar[int]
    DIALOG_CONTEXT_FIELD_NUMBER: _ClassVar[int]
    uuid: str
    dialog_context: DialogContext
    def __init__(self, uuid: _Optional[str] = ..., dialog_context: _Optional[_Union[DialogContext, _Mapping]] = ...) -> None: ...

class AppendSolutionContextRequest(_message.Message):
    __slots__ = ("uuid", "solution_context")
    UUID_FIELD_NUMBER: _ClassVar[int]
    SOLUTION_CONTEXT_FIELD_NUMBER: _ClassVar[int]
    uuid: str
    solution_context: SolutionContext
    def __init__(self, uuid: _Optional[str] = ..., solution_context: _Optional[_Union[SolutionContext, _Mapping]] = ...) -> None: ...

class GetFullContextRequest(_message.Message):
    __slots__ = ("uuid",)
    UUID_FIELD_NUMBER: _ClassVar[int]
    uuid: str
    def __init__(self, uuid: _Optional[str] = ...) -> None: ...

class GetNextDialupRequest(_message.Message):
    __slots__ = ("uuid", "current_dialup_id")
    UUID_FIELD_NUMBER: _ClassVar[int]
    CURRENT_DIALUP_ID_FIELD_NUMBER: _ClassVar[int]
    uuid: str
    current_dialup_id: str
    def __init__(self, uuid: _Optional[str] = ..., current_dialup_id: _Optional[str] = ...) -> None: ...

class GetDialupRequest(_message.Message):
    __slots__ = ("context_id", "dialup_id")
    CONTEXT_ID_FIELD_NUMBER: _ClassVar[int]
    DIALUP_ID_FIELD_NUMBER: _ClassVar[int]
    context_id: str
    dialup_id: str
    def __init__(self, context_id: _Optional[str] = ..., dialup_id: _Optional[str] = ...) -> None: ...

class GetDialogProgressRequest(_message.Message):
    __slots__ = ("context_id",)
    CONTEXT_ID_FIELD_NUMBER: _ClassVar[int]
    context_id: str
    def __init__(self, context_id: _Optional[str] = ...) -> None: ...

class StartDialupRequest(_message.Message):
    __slots__ = ("context_id", "dialup_id")
    CONTEXT_ID_FIELD_NUMBER: _ClassVar[int]
    DIALUP_ID_FIELD_NUMBER: _ClassVar[int]
    context_id: str
    dialup_id: str
    def __init__(self, context_id: _Optional[str] = ..., dialup_id: _Optional[str] = ...) -> None: ...

class CompleteDialupRequest(_message.Message):
    __slots__ = ("context_id", "dialup_id")
    CONTEXT_ID_FIELD_NUMBER: _ClassVar[int]
    DIALUP_ID_FIELD_NUMBER: _ClassVar[int]
    context_id: str
    dialup_id: str
    def __init__(self, context_id: _Optional[str] = ..., dialup_id: _Optional[str] = ...) -> None: ...

class DisconnectDialupRequest(_message.Message):
    __slots__ = ("context_id", "dialup_id")
    CONTEXT_ID_FIELD_NUMBER: _ClassVar[int]
    DIALUP_ID_FIELD_NUMBER: _ClassVar[int]
    context_id: str
    dialup_id: str
    def __init__(self, context_id: _Optional[str] = ..., dialup_id: _Optional[str] = ...) -> None: ...
