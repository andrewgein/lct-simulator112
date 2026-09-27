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

class DdsStageSignal(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    DDS_STAGE_SIGNAL_UNSPECIFIED: _ClassVar[DdsStageSignal]
    DDS_STAGE_SIGNAL_BRIGADE_ASSIGNED: _ClassVar[DdsStageSignal]
    DDS_STAGE_SIGNAL_BRIGADE_STATUS_CHANGED: _ClassVar[DdsStageSignal]
    DDS_STAGE_SIGNAL_STATUS_CALL_COMPLETED: _ClassVar[DdsStageSignal]
    DDS_STAGE_SIGNAL_ADDITIONAL_SERVICE_REQUESTED: _ClassVar[DdsStageSignal]
    DDS_STAGE_SIGNAL_INCIDENT_COMPLETED: _ClassVar[DdsStageSignal]

class ContextProgressStatus(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    CONTEXT_PROGRESS_STATUS_UNSPECIFIED: _ClassVar[ContextProgressStatus]
    CONTEXT_PROGRESS_STATUS_CREATED: _ClassVar[ContextProgressStatus]
    CONTEXT_PROGRESS_STATUS_FILLED: _ClassVar[ContextProgressStatus]
    CONTEXT_PROGRESS_STATUS_IN_REVIEW: _ClassVar[ContextProgressStatus]
    CONTEXT_PROGRESS_STATUS_DIALOG: _ClassVar[ContextProgressStatus]
    CONTEXT_PROGRESS_STATUS_DONE: _ClassVar[ContextProgressStatus]

class IncidentProgressStatus(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    INCIDENT_PROGRESS_STATUS_UNSPECIFIED: _ClassVar[IncidentProgressStatus]
    INCIDENT_PROGRESS_STATUS_PENDING: _ClassVar[IncidentProgressStatus]
    INCIDENT_PROGRESS_STATUS_ACTIVE: _ClassVar[IncidentProgressStatus]
    INCIDENT_PROGRESS_STATUS_COMPLETED: _ClassVar[IncidentProgressStatus]
    INCIDENT_PROGRESS_STATUS_FAILED: _ClassVar[IncidentProgressStatus]

class SpeakerType(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    USER: _ClassVar[SpeakerType]
    LLM: _ClassVar[SpeakerType]
DIALOG_PROGRESS_STATUS_UNSPECIFIED: DialogProgressStatus
IDLE: DialogProgressStatus
IN_CALL: DialogProgressStatus
DISCONNECTED: DialogProgressStatus
COMPLETED: DialogProgressStatus
DDS_STAGE_SIGNAL_UNSPECIFIED: DdsStageSignal
DDS_STAGE_SIGNAL_BRIGADE_ASSIGNED: DdsStageSignal
DDS_STAGE_SIGNAL_BRIGADE_STATUS_CHANGED: DdsStageSignal
DDS_STAGE_SIGNAL_STATUS_CALL_COMPLETED: DdsStageSignal
DDS_STAGE_SIGNAL_ADDITIONAL_SERVICE_REQUESTED: DdsStageSignal
DDS_STAGE_SIGNAL_INCIDENT_COMPLETED: DdsStageSignal
CONTEXT_PROGRESS_STATUS_UNSPECIFIED: ContextProgressStatus
CONTEXT_PROGRESS_STATUS_CREATED: ContextProgressStatus
CONTEXT_PROGRESS_STATUS_FILLED: ContextProgressStatus
CONTEXT_PROGRESS_STATUS_IN_REVIEW: ContextProgressStatus
CONTEXT_PROGRESS_STATUS_DIALOG: ContextProgressStatus
CONTEXT_PROGRESS_STATUS_DONE: ContextProgressStatus
INCIDENT_PROGRESS_STATUS_UNSPECIFIED: IncidentProgressStatus
INCIDENT_PROGRESS_STATUS_PENDING: IncidentProgressStatus
INCIDENT_PROGRESS_STATUS_ACTIVE: IncidentProgressStatus
INCIDENT_PROGRESS_STATUS_COMPLETED: IncidentProgressStatus
INCIDENT_PROGRESS_STATUS_FAILED: IncidentProgressStatus
USER: SpeakerType
LLM: SpeakerType

class PersonInfo(_message.Message):
    __slots__ = ("phone", "contact_phone", "last_name", "first_name", "middle_name", "address", "additional_info", "on_scene_phone", "status")
    PHONE_FIELD_NUMBER: _ClassVar[int]
    CONTACT_PHONE_FIELD_NUMBER: _ClassVar[int]
    LAST_NAME_FIELD_NUMBER: _ClassVar[int]
    FIRST_NAME_FIELD_NUMBER: _ClassVar[int]
    MIDDLE_NAME_FIELD_NUMBER: _ClassVar[int]
    ADDRESS_FIELD_NUMBER: _ClassVar[int]
    ADDITIONAL_INFO_FIELD_NUMBER: _ClassVar[int]
    ON_SCENE_PHONE_FIELD_NUMBER: _ClassVar[int]
    STATUS_FIELD_NUMBER: _ClassVar[int]
    phone: str
    contact_phone: str
    last_name: str
    first_name: str
    middle_name: str
    address: str
    additional_info: str
    on_scene_phone: str
    status: str
    def __init__(self, phone: _Optional[str] = ..., contact_phone: _Optional[str] = ..., last_name: _Optional[str] = ..., first_name: _Optional[str] = ..., middle_name: _Optional[str] = ..., address: _Optional[str] = ..., additional_info: _Optional[str] = ..., on_scene_phone: _Optional[str] = ..., status: _Optional[str] = ...) -> None: ...

class DialogProgress(_message.Message):
    __slots__ = ("context_id", "active_call_id", "status")
    CONTEXT_ID_FIELD_NUMBER: _ClassVar[int]
    ACTIVE_CALL_ID_FIELD_NUMBER: _ClassVar[int]
    STATUS_FIELD_NUMBER: _ClassVar[int]
    context_id: str
    active_call_id: str
    status: DialogProgressStatus
    def __init__(self, context_id: _Optional[str] = ..., active_call_id: _Optional[str] = ..., status: _Optional[_Union[DialogProgressStatus, str]] = ...) -> None: ...

class System112Progress(_message.Message):
    __slots__ = ("active_call_id", "completed_calls", "total_calls")
    ACTIVE_CALL_ID_FIELD_NUMBER: _ClassVar[int]
    COMPLETED_CALLS_FIELD_NUMBER: _ClassVar[int]
    TOTAL_CALLS_FIELD_NUMBER: _ClassVar[int]
    active_call_id: str
    completed_calls: int
    total_calls: int
    def __init__(self, active_call_id: _Optional[str] = ..., completed_calls: _Optional[int] = ..., total_calls: _Optional[int] = ...) -> None: ...

class DdsStageProgress(_message.Message):
    __slots__ = ("stage_id", "stage_type", "status", "started_at", "deadline")
    STAGE_ID_FIELD_NUMBER: _ClassVar[int]
    STAGE_TYPE_FIELD_NUMBER: _ClassVar[int]
    STATUS_FIELD_NUMBER: _ClassVar[int]
    STARTED_AT_FIELD_NUMBER: _ClassVar[int]
    DEADLINE_FIELD_NUMBER: _ClassVar[int]
    stage_id: str
    stage_type: str
    status: str
    started_at: str
    deadline: str
    def __init__(self, stage_id: _Optional[str] = ..., stage_type: _Optional[str] = ..., status: _Optional[str] = ..., started_at: _Optional[str] = ..., deadline: _Optional[str] = ...) -> None: ...

class DdsProgress(_message.Message):
    __slots__ = ("active_stage_id", "deadline", "stages")
    ACTIVE_STAGE_ID_FIELD_NUMBER: _ClassVar[int]
    DEADLINE_FIELD_NUMBER: _ClassVar[int]
    STAGES_FIELD_NUMBER: _ClassVar[int]
    active_stage_id: str
    deadline: str
    stages: _containers.RepeatedCompositeFieldContainer[DdsStageProgress]
    def __init__(self, active_stage_id: _Optional[str] = ..., deadline: _Optional[str] = ..., stages: _Optional[_Iterable[_Union[DdsStageProgress, _Mapping]]] = ...) -> None: ...

class IncidentProgress(_message.Message):
    __slots__ = ("incident_id", "status", "system_112", "dds")
    INCIDENT_ID_FIELD_NUMBER: _ClassVar[int]
    STATUS_FIELD_NUMBER: _ClassVar[int]
    SYSTEM_112_FIELD_NUMBER: _ClassVar[int]
    DDS_FIELD_NUMBER: _ClassVar[int]
    incident_id: str
    status: IncidentProgressStatus
    system_112: System112Progress
    dds: DdsProgress
    def __init__(self, incident_id: _Optional[str] = ..., status: _Optional[_Union[IncidentProgressStatus, str]] = ..., system_112: _Optional[_Union[System112Progress, _Mapping]] = ..., dds: _Optional[_Union[DdsProgress, _Mapping]] = ...) -> None: ...

class LevelProgress(_message.Message):
    __slots__ = ("context_id", "target_type", "execution_mode", "status", "incidents")
    CONTEXT_ID_FIELD_NUMBER: _ClassVar[int]
    TARGET_TYPE_FIELD_NUMBER: _ClassVar[int]
    EXECUTION_MODE_FIELD_NUMBER: _ClassVar[int]
    STATUS_FIELD_NUMBER: _ClassVar[int]
    INCIDENTS_FIELD_NUMBER: _ClassVar[int]
    context_id: str
    target_type: _incident_context_pb2.IncidentTargetType
    execution_mode: _incident_context_pb2.ExecutionMode
    status: ContextProgressStatus
    incidents: _containers.RepeatedCompositeFieldContainer[IncidentProgress]
    def __init__(self, context_id: _Optional[str] = ..., target_type: _Optional[_Union[_incident_context_pb2.IncidentTargetType, str]] = ..., execution_mode: _Optional[_Union[_incident_context_pb2.ExecutionMode, str]] = ..., status: _Optional[_Union[ContextProgressStatus, str]] = ..., incidents: _Optional[_Iterable[_Union[IncidentProgress, _Mapping]]] = ...) -> None: ...

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
    __slots__ = ("applicant", "victim_count", "additional_info", "incident_types", "call_id", "main_card_id", "revision_id", "card_id", "previous_revision_id", "version", "created_at", "additional_info_provided", "services")
    class AdditionalInfoEntry(_message.Message):
        __slots__ = ("key", "value")
        KEY_FIELD_NUMBER: _ClassVar[int]
        VALUE_FIELD_NUMBER: _ClassVar[int]
        key: str
        value: str
        def __init__(self, key: _Optional[str] = ..., value: _Optional[str] = ...) -> None: ...
    APPLICANT_FIELD_NUMBER: _ClassVar[int]
    VICTIM_COUNT_FIELD_NUMBER: _ClassVar[int]
    ADDITIONAL_INFO_FIELD_NUMBER: _ClassVar[int]
    INCIDENT_TYPES_FIELD_NUMBER: _ClassVar[int]
    CALL_ID_FIELD_NUMBER: _ClassVar[int]
    MAIN_CARD_ID_FIELD_NUMBER: _ClassVar[int]
    REVISION_ID_FIELD_NUMBER: _ClassVar[int]
    CARD_ID_FIELD_NUMBER: _ClassVar[int]
    PREVIOUS_REVISION_ID_FIELD_NUMBER: _ClassVar[int]
    VERSION_FIELD_NUMBER: _ClassVar[int]
    CREATED_AT_FIELD_NUMBER: _ClassVar[int]
    ADDITIONAL_INFO_PROVIDED_FIELD_NUMBER: _ClassVar[int]
    SERVICES_FIELD_NUMBER: _ClassVar[int]
    applicant: PersonInfo
    victim_count: int
    additional_info: _containers.ScalarMap[str, str]
    incident_types: _containers.RepeatedScalarFieldContainer[str]
    call_id: str
    main_card_id: str
    revision_id: str
    card_id: str
    previous_revision_id: str
    version: int
    created_at: str
    additional_info_provided: bool
    services: _containers.RepeatedScalarFieldContainer[str]
    def __init__(self, applicant: _Optional[_Union[PersonInfo, _Mapping]] = ..., victim_count: _Optional[int] = ..., additional_info: _Optional[_Mapping[str, str]] = ..., incident_types: _Optional[_Iterable[str]] = ..., call_id: _Optional[str] = ..., main_card_id: _Optional[str] = ..., revision_id: _Optional[str] = ..., card_id: _Optional[str] = ..., previous_revision_id: _Optional[str] = ..., version: _Optional[int] = ..., created_at: _Optional[str] = ..., additional_info_provided: _Optional[bool] = ..., services: _Optional[_Iterable[str]] = ...) -> None: ...

class AssignmentContext(_message.Message):
    __slots__ = ("assignment_id", "user_id", "title", "target_type", "difficulty", "execution_mode", "incidents", "threshold_3", "threshold_4", "threshold_5")
    ASSIGNMENT_ID_FIELD_NUMBER: _ClassVar[int]
    USER_ID_FIELD_NUMBER: _ClassVar[int]
    TITLE_FIELD_NUMBER: _ClassVar[int]
    TARGET_TYPE_FIELD_NUMBER: _ClassVar[int]
    DIFFICULTY_FIELD_NUMBER: _ClassVar[int]
    EXECUTION_MODE_FIELD_NUMBER: _ClassVar[int]
    INCIDENTS_FIELD_NUMBER: _ClassVar[int]
    THRESHOLD_3_FIELD_NUMBER: _ClassVar[int]
    THRESHOLD_4_FIELD_NUMBER: _ClassVar[int]
    THRESHOLD_5_FIELD_NUMBER: _ClassVar[int]
    assignment_id: str
    user_id: str
    title: str
    target_type: _incident_context_pb2.IncidentTargetType
    difficulty: _incident_context_pb2.Difficulty
    execution_mode: _incident_context_pb2.ExecutionMode
    incidents: _containers.RepeatedCompositeFieldContainer[_incident_context_pb2.IncidentContext]
    threshold_3: int
    threshold_4: int
    threshold_5: int
    def __init__(self, assignment_id: _Optional[str] = ..., user_id: _Optional[str] = ..., title: _Optional[str] = ..., target_type: _Optional[_Union[_incident_context_pb2.IncidentTargetType, str]] = ..., difficulty: _Optional[_Union[_incident_context_pb2.Difficulty, str]] = ..., execution_mode: _Optional[_Union[_incident_context_pb2.ExecutionMode, str]] = ..., incidents: _Optional[_Iterable[_Union[_incident_context_pb2.IncidentContext, _Mapping]]] = ..., threshold_3: _Optional[int] = ..., threshold_4: _Optional[int] = ..., threshold_5: _Optional[int] = ...) -> None: ...

class FullContext(_message.Message):
    __slots__ = ("uuid", "user_id", "incident_context", "dialog_context", "solution_context", "assignment_context", "solution_context_revisions", "level_progress", "started_at", "submitted_at")
    UUID_FIELD_NUMBER: _ClassVar[int]
    USER_ID_FIELD_NUMBER: _ClassVar[int]
    INCIDENT_CONTEXT_FIELD_NUMBER: _ClassVar[int]
    DIALOG_CONTEXT_FIELD_NUMBER: _ClassVar[int]
    SOLUTION_CONTEXT_FIELD_NUMBER: _ClassVar[int]
    ASSIGNMENT_CONTEXT_FIELD_NUMBER: _ClassVar[int]
    SOLUTION_CONTEXT_REVISIONS_FIELD_NUMBER: _ClassVar[int]
    LEVEL_PROGRESS_FIELD_NUMBER: _ClassVar[int]
    STARTED_AT_FIELD_NUMBER: _ClassVar[int]
    SUBMITTED_AT_FIELD_NUMBER: _ClassVar[int]
    uuid: str
    user_id: str
    incident_context: _incident_context_pb2.IncidentContext
    dialog_context: DialogContext
    solution_context: SolutionContext
    assignment_context: AssignmentContext
    solution_context_revisions: _containers.RepeatedCompositeFieldContainer[SolutionContext]
    level_progress: LevelProgress
    started_at: str
    submitted_at: str
    def __init__(self, uuid: _Optional[str] = ..., user_id: _Optional[str] = ..., incident_context: _Optional[_Union[_incident_context_pb2.IncidentContext, _Mapping]] = ..., dialog_context: _Optional[_Union[DialogContext, _Mapping]] = ..., solution_context: _Optional[_Union[SolutionContext, _Mapping]] = ..., assignment_context: _Optional[_Union[AssignmentContext, _Mapping]] = ..., solution_context_revisions: _Optional[_Iterable[_Union[SolutionContext, _Mapping]]] = ..., level_progress: _Optional[_Union[LevelProgress, _Mapping]] = ..., started_at: _Optional[str] = ..., submitted_at: _Optional[str] = ...) -> None: ...

class GetIncidentContextRequest(_message.Message):
    __slots__ = ("uuid",)
    UUID_FIELD_NUMBER: _ClassVar[int]
    uuid: str
    def __init__(self, uuid: _Optional[str] = ...) -> None: ...

class AppendDialogContextRequest(_message.Message):
    __slots__ = ("uuid", "dialog_context", "call_id")
    UUID_FIELD_NUMBER: _ClassVar[int]
    DIALOG_CONTEXT_FIELD_NUMBER: _ClassVar[int]
    CALL_ID_FIELD_NUMBER: _ClassVar[int]
    uuid: str
    dialog_context: DialogContext
    call_id: str
    def __init__(self, uuid: _Optional[str] = ..., dialog_context: _Optional[_Union[DialogContext, _Mapping]] = ..., call_id: _Optional[str] = ...) -> None: ...

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

class GetNextCallRequest(_message.Message):
    __slots__ = ("uuid", "current_call_id")
    UUID_FIELD_NUMBER: _ClassVar[int]
    CURRENT_CALL_ID_FIELD_NUMBER: _ClassVar[int]
    uuid: str
    current_call_id: str
    def __init__(self, uuid: _Optional[str] = ..., current_call_id: _Optional[str] = ...) -> None: ...

class GetCallRequest(_message.Message):
    __slots__ = ("context_id", "call_id")
    CONTEXT_ID_FIELD_NUMBER: _ClassVar[int]
    CALL_ID_FIELD_NUMBER: _ClassVar[int]
    context_id: str
    call_id: str
    def __init__(self, context_id: _Optional[str] = ..., call_id: _Optional[str] = ...) -> None: ...

class GetDialogProgressRequest(_message.Message):
    __slots__ = ("context_id",)
    CONTEXT_ID_FIELD_NUMBER: _ClassVar[int]
    context_id: str
    def __init__(self, context_id: _Optional[str] = ...) -> None: ...

class StartCallRequest(_message.Message):
    __slots__ = ("context_id", "call_id")
    CONTEXT_ID_FIELD_NUMBER: _ClassVar[int]
    CALL_ID_FIELD_NUMBER: _ClassVar[int]
    context_id: str
    call_id: str
    def __init__(self, context_id: _Optional[str] = ..., call_id: _Optional[str] = ...) -> None: ...

class CompleteCallRequest(_message.Message):
    __slots__ = ("context_id", "call_id")
    CONTEXT_ID_FIELD_NUMBER: _ClassVar[int]
    CALL_ID_FIELD_NUMBER: _ClassVar[int]
    context_id: str
    call_id: str
    def __init__(self, context_id: _Optional[str] = ..., call_id: _Optional[str] = ...) -> None: ...

class DisconnectCallRequest(_message.Message):
    __slots__ = ("context_id", "call_id")
    CONTEXT_ID_FIELD_NUMBER: _ClassVar[int]
    CALL_ID_FIELD_NUMBER: _ClassVar[int]
    context_id: str
    call_id: str
    def __init__(self, context_id: _Optional[str] = ..., call_id: _Optional[str] = ...) -> None: ...

class GetCallTranscriptRequest(_message.Message):
    __slots__ = ("context_id", "call_id")
    CONTEXT_ID_FIELD_NUMBER: _ClassVar[int]
    CALL_ID_FIELD_NUMBER: _ClassVar[int]
    context_id: str
    call_id: str
    def __init__(self, context_id: _Optional[str] = ..., call_id: _Optional[str] = ...) -> None: ...

class ClearCallTranscriptRequest(_message.Message):
    __slots__ = ("context_id", "call_id")
    CONTEXT_ID_FIELD_NUMBER: _ClassVar[int]
    CALL_ID_FIELD_NUMBER: _ClassVar[int]
    context_id: str
    call_id: str
    def __init__(self, context_id: _Optional[str] = ..., call_id: _Optional[str] = ...) -> None: ...

class GetLevelProgressRequest(_message.Message):
    __slots__ = ("context_id",)
    CONTEXT_ID_FIELD_NUMBER: _ClassVar[int]
    context_id: str
    def __init__(self, context_id: _Optional[str] = ...) -> None: ...

class ApplyDdsStageSignalRequest(_message.Message):
    __slots__ = ("context_id", "incident_id", "signal")
    CONTEXT_ID_FIELD_NUMBER: _ClassVar[int]
    INCIDENT_ID_FIELD_NUMBER: _ClassVar[int]
    SIGNAL_FIELD_NUMBER: _ClassVar[int]
    context_id: str
    incident_id: str
    signal: DdsStageSignal
    def __init__(self, context_id: _Optional[str] = ..., incident_id: _Optional[str] = ..., signal: _Optional[_Union[DdsStageSignal, str]] = ...) -> None: ...
