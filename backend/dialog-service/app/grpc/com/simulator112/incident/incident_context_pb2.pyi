from app.grpc.com.simulator112.common import dds_service_pb2 as _dds_service_pb2
from google.protobuf.internal import containers as _containers
from google.protobuf.internal import enum_type_wrapper as _enum_type_wrapper
from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from collections.abc import Iterable as _Iterable, Mapping as _Mapping
from typing import ClassVar as _ClassVar, Optional as _Optional, Union as _Union

DESCRIPTOR: _descriptor.FileDescriptor

class ExecutionMode(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    EXECUTION_MODE_UNSPECIFIED: _ClassVar[ExecutionMode]
    EXECUTION_MODE_SEQUENTIAL: _ClassVar[ExecutionMode]
    EXECUTION_MODE_PARALLEL: _ClassVar[ExecutionMode]

class IncidentTargetType(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    INCIDENT_TARGET_TYPE_UNSPECIFIED: _ClassVar[IncidentTargetType]
    INCIDENT_TARGET_TYPE_SYSTEM_112: _ClassVar[IncidentTargetType]
    INCIDENT_TARGET_TYPE_DDS: _ClassVar[IncidentTargetType]

class Difficulty(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    DIFFICULTY_UNSPECIFIED: _ClassVar[Difficulty]
    DIFFICULTY_EASY: _ClassVar[Difficulty]
    DIFFICULTY_NORMAL: _ClassVar[Difficulty]
    DIFFICULTY_HARD: _ClassVar[Difficulty]

class Gender(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    GENDER_UNSPECIFIED: _ClassVar[Gender]
    GENDER_MAN: _ClassVar[Gender]
    GENDER_WOMEN: _ClassVar[Gender]

class DdsStageType(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    DDS_STAGE_TYPE_UNSPECIFIED: _ClassVar[DdsStageType]
    DDS_STAGE_TYPE_ASSIGN_BRIGADE: _ClassVar[DdsStageType]
    DDS_STAGE_TYPE_WAIT_FOR_BRIGADE_STATUS_CHANGE: _ClassVar[DdsStageType]
    DDS_STAGE_TYPE_CALL_BRIGADE_FOR_STATUS: _ClassVar[DdsStageType]
    DDS_STAGE_TYPE_REQUEST_ADDITIONAL_SERVICE: _ClassVar[DdsStageType]
    DDS_STAGE_TYPE_COMPLETE_INCIDENT: _ClassVar[DdsStageType]

class CallDirection(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    CALL_DIRECTION_UNSPECIFIED: _ClassVar[CallDirection]
    CALL_DIRECTION_INBOUND: _ClassVar[CallDirection]
    CALL_DIRECTION_OUTBOUND: _ClassVar[CallDirection]

class CounterpartyType(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    COUNTERPARTY_TYPE_UNSPECIFIED: _ClassVar[CounterpartyType]
    COUNTERPARTY_TYPE_CALLER: _ClassVar[CounterpartyType]
    COUNTERPARTY_TYPE_BRIGADE: _ClassVar[CounterpartyType]
EXECUTION_MODE_UNSPECIFIED: ExecutionMode
EXECUTION_MODE_SEQUENTIAL: ExecutionMode
EXECUTION_MODE_PARALLEL: ExecutionMode
INCIDENT_TARGET_TYPE_UNSPECIFIED: IncidentTargetType
INCIDENT_TARGET_TYPE_SYSTEM_112: IncidentTargetType
INCIDENT_TARGET_TYPE_DDS: IncidentTargetType
DIFFICULTY_UNSPECIFIED: Difficulty
DIFFICULTY_EASY: Difficulty
DIFFICULTY_NORMAL: Difficulty
DIFFICULTY_HARD: Difficulty
GENDER_UNSPECIFIED: Gender
GENDER_MAN: Gender
GENDER_WOMEN: Gender
DDS_STAGE_TYPE_UNSPECIFIED: DdsStageType
DDS_STAGE_TYPE_ASSIGN_BRIGADE: DdsStageType
DDS_STAGE_TYPE_WAIT_FOR_BRIGADE_STATUS_CHANGE: DdsStageType
DDS_STAGE_TYPE_CALL_BRIGADE_FOR_STATUS: DdsStageType
DDS_STAGE_TYPE_REQUEST_ADDITIONAL_SERVICE: DdsStageType
DDS_STAGE_TYPE_COMPLETE_INCIDENT: DdsStageType
CALL_DIRECTION_UNSPECIFIED: CallDirection
CALL_DIRECTION_INBOUND: CallDirection
CALL_DIRECTION_OUTBOUND: CallDirection
COUNTERPARTY_TYPE_UNSPECIFIED: CounterpartyType
COUNTERPARTY_TYPE_CALLER: CounterpartyType
COUNTERPARTY_TYPE_BRIGADE: CounterpartyType

class LevelContext(_message.Message):
    __slots__ = ("id", "title", "target_type", "difficulty", "execution_mode", "incidents")
    ID_FIELD_NUMBER: _ClassVar[int]
    TITLE_FIELD_NUMBER: _ClassVar[int]
    TARGET_TYPE_FIELD_NUMBER: _ClassVar[int]
    DIFFICULTY_FIELD_NUMBER: _ClassVar[int]
    EXECUTION_MODE_FIELD_NUMBER: _ClassVar[int]
    INCIDENTS_FIELD_NUMBER: _ClassVar[int]
    id: str
    title: str
    target_type: IncidentTargetType
    difficulty: Difficulty
    execution_mode: ExecutionMode
    incidents: _containers.RepeatedCompositeFieldContainer[IncidentContext]
    def __init__(self, id: _Optional[str] = ..., title: _Optional[str] = ..., target_type: _Optional[_Union[IncidentTargetType, str]] = ..., difficulty: _Optional[_Union[Difficulty, str]] = ..., execution_mode: _Optional[_Union[ExecutionMode, str]] = ..., incidents: _Optional[_Iterable[_Union[IncidentContext, _Mapping]]] = ...) -> None: ...

class IncidentContext(_message.Message):
    __slots__ = ("id", "title", "address", "difficulty", "target_type", "stages", "criteria", "prepared_card_template", "initial_assignment", "dds_initial_stage_id", "dds_stage_transitions")
    ID_FIELD_NUMBER: _ClassVar[int]
    TITLE_FIELD_NUMBER: _ClassVar[int]
    ADDRESS_FIELD_NUMBER: _ClassVar[int]
    DIFFICULTY_FIELD_NUMBER: _ClassVar[int]
    TARGET_TYPE_FIELD_NUMBER: _ClassVar[int]
    STAGES_FIELD_NUMBER: _ClassVar[int]
    CRITERIA_FIELD_NUMBER: _ClassVar[int]
    PREPARED_CARD_TEMPLATE_FIELD_NUMBER: _ClassVar[int]
    INITIAL_ASSIGNMENT_FIELD_NUMBER: _ClassVar[int]
    DDS_INITIAL_STAGE_ID_FIELD_NUMBER: _ClassVar[int]
    DDS_STAGE_TRANSITIONS_FIELD_NUMBER: _ClassVar[int]
    id: str
    title: str
    address: Address
    difficulty: Difficulty
    target_type: IncidentTargetType
    stages: _containers.RepeatedCompositeFieldContainer[IncidentStage]
    criteria: Criteria
    prepared_card_template: PreparedCardTemplate
    initial_assignment: InitialAssignment
    dds_initial_stage_id: str
    dds_stage_transitions: _containers.RepeatedCompositeFieldContainer[DdsStageTransition]
    def __init__(self, id: _Optional[str] = ..., title: _Optional[str] = ..., address: _Optional[_Union[Address, _Mapping]] = ..., difficulty: _Optional[_Union[Difficulty, str]] = ..., target_type: _Optional[_Union[IncidentTargetType, str]] = ..., stages: _Optional[_Iterable[_Union[IncidentStage, _Mapping]]] = ..., criteria: _Optional[_Union[Criteria, _Mapping]] = ..., prepared_card_template: _Optional[_Union[PreparedCardTemplate, _Mapping]] = ..., initial_assignment: _Optional[_Union[InitialAssignment, _Mapping]] = ..., dds_initial_stage_id: _Optional[str] = ..., dds_stage_transitions: _Optional[_Iterable[_Union[DdsStageTransition, _Mapping]]] = ...) -> None: ...

class IncidentStage(_message.Message):
    __slots__ = ("id", "title", "description", "calls", "system_112", "dds")
    ID_FIELD_NUMBER: _ClassVar[int]
    TITLE_FIELD_NUMBER: _ClassVar[int]
    DESCRIPTION_FIELD_NUMBER: _ClassVar[int]
    CALLS_FIELD_NUMBER: _ClassVar[int]
    SYSTEM_112_FIELD_NUMBER: _ClassVar[int]
    DDS_FIELD_NUMBER: _ClassVar[int]
    id: str
    title: str
    description: str
    calls: _containers.RepeatedCompositeFieldContainer[CallScenario]
    system_112: System112StageDetails
    dds: DdsStageDetails
    def __init__(self, id: _Optional[str] = ..., title: _Optional[str] = ..., description: _Optional[str] = ..., calls: _Optional[_Iterable[_Union[CallScenario, _Mapping]]] = ..., system_112: _Optional[_Union[System112StageDetails, _Mapping]] = ..., dds: _Optional[_Union[DdsStageDetails, _Mapping]] = ...) -> None: ...

class System112StageDetails(_message.Message):
    __slots__ = ("classifier_codes", "victim_count", "position")
    CLASSIFIER_CODES_FIELD_NUMBER: _ClassVar[int]
    VICTIM_COUNT_FIELD_NUMBER: _ClassVar[int]
    POSITION_FIELD_NUMBER: _ClassVar[int]
    classifier_codes: _containers.RepeatedScalarFieldContainer[str]
    victim_count: int
    position: int
    def __init__(self, classifier_codes: _Optional[_Iterable[str]] = ..., victim_count: _Optional[int] = ..., position: _Optional[int] = ...) -> None: ...

class DdsStageDetails(_message.Message):
    __slots__ = ("type", "time_limit_seconds")
    TYPE_FIELD_NUMBER: _ClassVar[int]
    TIME_LIMIT_SECONDS_FIELD_NUMBER: _ClassVar[int]
    type: DdsStageType
    time_limit_seconds: int
    def __init__(self, type: _Optional[_Union[DdsStageType, str]] = ..., time_limit_seconds: _Optional[int] = ...) -> None: ...

class CallScenario(_message.Message):
    __slots__ = ("id", "position", "direction", "counterparty", "person", "gender", "known_facts", "hidden_facts", "ai_context", "emotional_state")
    ID_FIELD_NUMBER: _ClassVar[int]
    POSITION_FIELD_NUMBER: _ClassVar[int]
    DIRECTION_FIELD_NUMBER: _ClassVar[int]
    COUNTERPARTY_FIELD_NUMBER: _ClassVar[int]
    PERSON_FIELD_NUMBER: _ClassVar[int]
    GENDER_FIELD_NUMBER: _ClassVar[int]
    KNOWN_FACTS_FIELD_NUMBER: _ClassVar[int]
    HIDDEN_FACTS_FIELD_NUMBER: _ClassVar[int]
    AI_CONTEXT_FIELD_NUMBER: _ClassVar[int]
    EMOTIONAL_STATE_FIELD_NUMBER: _ClassVar[int]
    id: str
    position: int
    direction: CallDirection
    counterparty: CounterpartyType
    person: Person
    gender: Gender
    known_facts: _containers.RepeatedScalarFieldContainer[str]
    hidden_facts: _containers.RepeatedScalarFieldContainer[str]
    ai_context: str
    emotional_state: str
    def __init__(self, id: _Optional[str] = ..., position: _Optional[int] = ..., direction: _Optional[_Union[CallDirection, str]] = ..., counterparty: _Optional[_Union[CounterpartyType, str]] = ..., person: _Optional[_Union[Person, _Mapping]] = ..., gender: _Optional[_Union[Gender, str]] = ..., known_facts: _Optional[_Iterable[str]] = ..., hidden_facts: _Optional[_Iterable[str]] = ..., ai_context: _Optional[str] = ..., emotional_state: _Optional[str] = ...) -> None: ...

class PreparedCardTemplate(_message.Message):
    __slots__ = ("classifier_codes", "applicant", "victim_count", "additional_info")
    class AdditionalInfoEntry(_message.Message):
        __slots__ = ("key", "value")
        KEY_FIELD_NUMBER: _ClassVar[int]
        VALUE_FIELD_NUMBER: _ClassVar[int]
        key: str
        value: str
        def __init__(self, key: _Optional[str] = ..., value: _Optional[str] = ...) -> None: ...
    CLASSIFIER_CODES_FIELD_NUMBER: _ClassVar[int]
    APPLICANT_FIELD_NUMBER: _ClassVar[int]
    VICTIM_COUNT_FIELD_NUMBER: _ClassVar[int]
    ADDITIONAL_INFO_FIELD_NUMBER: _ClassVar[int]
    classifier_codes: _containers.RepeatedScalarFieldContainer[str]
    applicant: Person
    victim_count: int
    additional_info: _containers.ScalarMap[str, str]
    def __init__(self, classifier_codes: _Optional[_Iterable[str]] = ..., applicant: _Optional[_Union[Person, _Mapping]] = ..., victim_count: _Optional[int] = ..., additional_info: _Optional[_Mapping[str, str]] = ...) -> None: ...

class InitialAssignment(_message.Message):
    __slots__ = ("emergency_service", "classifier_code", "instructions")
    EMERGENCY_SERVICE_FIELD_NUMBER: _ClassVar[int]
    CLASSIFIER_CODE_FIELD_NUMBER: _ClassVar[int]
    INSTRUCTIONS_FIELD_NUMBER: _ClassVar[int]
    emergency_service: _dds_service_pb2.DdsService
    classifier_code: str
    instructions: str
    def __init__(self, emergency_service: _Optional[_Union[_dds_service_pb2.DdsService, str]] = ..., classifier_code: _Optional[str] = ..., instructions: _Optional[str] = ...) -> None: ...

class DdsStageTransition(_message.Message):
    __slots__ = ("stage_id", "success_stage_id", "failure_stage_id")
    STAGE_ID_FIELD_NUMBER: _ClassVar[int]
    SUCCESS_STAGE_ID_FIELD_NUMBER: _ClassVar[int]
    FAILURE_STAGE_ID_FIELD_NUMBER: _ClassVar[int]
    stage_id: str
    success_stage_id: str
    failure_stage_id: str
    def __init__(self, stage_id: _Optional[str] = ..., success_stage_id: _Optional[str] = ..., failure_stage_id: _Optional[str] = ...) -> None: ...

class Criteria(_message.Message):
    __slots__ = ("required_questions", "expected_actions", "critical_mistakes")
    REQUIRED_QUESTIONS_FIELD_NUMBER: _ClassVar[int]
    EXPECTED_ACTIONS_FIELD_NUMBER: _ClassVar[int]
    CRITICAL_MISTAKES_FIELD_NUMBER: _ClassVar[int]
    required_questions: _containers.RepeatedScalarFieldContainer[str]
    expected_actions: _containers.RepeatedScalarFieldContainer[str]
    critical_mistakes: _containers.RepeatedScalarFieldContainer[str]
    def __init__(self, required_questions: _Optional[_Iterable[str]] = ..., expected_actions: _Optional[_Iterable[str]] = ..., critical_mistakes: _Optional[_Iterable[str]] = ...) -> None: ...

class Address(_message.Message):
    __slots__ = ("city", "street", "house", "building", "apartment", "floor")
    CITY_FIELD_NUMBER: _ClassVar[int]
    STREET_FIELD_NUMBER: _ClassVar[int]
    HOUSE_FIELD_NUMBER: _ClassVar[int]
    BUILDING_FIELD_NUMBER: _ClassVar[int]
    APARTMENT_FIELD_NUMBER: _ClassVar[int]
    FLOOR_FIELD_NUMBER: _ClassVar[int]
    city: str
    street: str
    house: str
    building: str
    apartment: str
    floor: int
    def __init__(self, city: _Optional[str] = ..., street: _Optional[str] = ..., house: _Optional[str] = ..., building: _Optional[str] = ..., apartment: _Optional[str] = ..., floor: _Optional[int] = ...) -> None: ...

class Person(_message.Message):
    __slots__ = ("first_name", "last_name", "middle_name", "age", "phone", "contact_phone", "address", "additional_info")
    FIRST_NAME_FIELD_NUMBER: _ClassVar[int]
    LAST_NAME_FIELD_NUMBER: _ClassVar[int]
    MIDDLE_NAME_FIELD_NUMBER: _ClassVar[int]
    AGE_FIELD_NUMBER: _ClassVar[int]
    PHONE_FIELD_NUMBER: _ClassVar[int]
    CONTACT_PHONE_FIELD_NUMBER: _ClassVar[int]
    ADDRESS_FIELD_NUMBER: _ClassVar[int]
    ADDITIONAL_INFO_FIELD_NUMBER: _ClassVar[int]
    first_name: str
    last_name: str
    middle_name: str
    age: int
    phone: str
    contact_phone: str
    address: str
    additional_info: str
    def __init__(self, first_name: _Optional[str] = ..., last_name: _Optional[str] = ..., middle_name: _Optional[str] = ..., age: _Optional[int] = ..., phone: _Optional[str] = ..., contact_phone: _Optional[str] = ..., address: _Optional[str] = ..., additional_info: _Optional[str] = ...) -> None: ...
