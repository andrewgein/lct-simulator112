from google.protobuf.internal import containers as _containers
from google.protobuf.internal import enum_type_wrapper as _enum_type_wrapper
from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from typing import ClassVar as _ClassVar, Iterable as _Iterable, Mapping as _Mapping, Optional as _Optional, Union as _Union

DESCRIPTOR: _descriptor.FileDescriptor

class DdsCompletionTrigger(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    DDS_COMPLETION_TRIGGER_UNSPECIFIED: _ClassVar[DdsCompletionTrigger]
    DDS_COMPLETION_TRIGGER_TIME: _ClassVar[DdsCompletionTrigger]
    DDS_COMPLETION_TRIGGER_STATUS: _ClassVar[DdsCompletionTrigger]
    DDS_COMPLETION_TRIGGER_CALLS: _ClassVar[DdsCompletionTrigger]

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

class IncidentStatus(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    INCIDENT_STATUS_UNSPECIFIED: _ClassVar[IncidentStatus]
    INCIDENT_STATUS_ADDED: _ClassVar[IncidentStatus]
    INCIDENT_STATUS_RECEIVED_BY_SERVICE: _ClassVar[IncidentStatus]
    INCIDENT_STATUS_ACCEPTED: _ClassVar[IncidentStatus]
    INCIDENT_STATUS_NOT_ACCEPTED: _ClassVar[IncidentStatus]
    INCIDENT_STATUS_RESPONSE_STARTED: _ClassVar[IncidentStatus]
    INCIDENT_STATUS_ARRIVED: _ClassVar[IncidentStatus]
    INCIDENT_STATUS_WORK_IN_PROGRESS: _ClassVar[IncidentStatus]
    INCIDENT_STATUS_WORK_COMPLETED: _ClassVar[IncidentStatus]
    INCIDENT_STATUS_WORK_REFUSED: _ClassVar[IncidentStatus]
    INCIDENT_STATUS_REGISTERED: _ClassVar[IncidentStatus]
    INCIDENT_STATUS_PROCESSED: _ClassVar[IncidentStatus]
    INCIDENT_STATUS_VERIFIED: _ClassVar[IncidentStatus]
    INCIDENT_STATUS_NOT_NOTIFIED: _ClassVar[IncidentStatus]
    INCIDENT_STATUS_REFUSED: _ClassVar[IncidentStatus]
    INCIDENT_STATUS_NOT_COMPLETED: _ClassVar[IncidentStatus]
    INCIDENT_STATUS_COMPLETED: _ClassVar[IncidentStatus]

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
    COUNTERPARTY_TYPE_SERVICE: _ClassVar[CounterpartyType]
DDS_COMPLETION_TRIGGER_UNSPECIFIED: DdsCompletionTrigger
DDS_COMPLETION_TRIGGER_TIME: DdsCompletionTrigger
DDS_COMPLETION_TRIGGER_STATUS: DdsCompletionTrigger
DDS_COMPLETION_TRIGGER_CALLS: DdsCompletionTrigger
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
INCIDENT_STATUS_UNSPECIFIED: IncidentStatus
INCIDENT_STATUS_ADDED: IncidentStatus
INCIDENT_STATUS_RECEIVED_BY_SERVICE: IncidentStatus
INCIDENT_STATUS_ACCEPTED: IncidentStatus
INCIDENT_STATUS_NOT_ACCEPTED: IncidentStatus
INCIDENT_STATUS_RESPONSE_STARTED: IncidentStatus
INCIDENT_STATUS_ARRIVED: IncidentStatus
INCIDENT_STATUS_WORK_IN_PROGRESS: IncidentStatus
INCIDENT_STATUS_WORK_COMPLETED: IncidentStatus
INCIDENT_STATUS_WORK_REFUSED: IncidentStatus
INCIDENT_STATUS_REGISTERED: IncidentStatus
INCIDENT_STATUS_PROCESSED: IncidentStatus
INCIDENT_STATUS_VERIFIED: IncidentStatus
INCIDENT_STATUS_NOT_NOTIFIED: IncidentStatus
INCIDENT_STATUS_REFUSED: IncidentStatus
INCIDENT_STATUS_NOT_COMPLETED: IncidentStatus
INCIDENT_STATUS_COMPLETED: IncidentStatus
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
COUNTERPARTY_TYPE_SERVICE: CounterpartyType

class IncidentContext(_message.Message):
    __slots__ = ("id", "title", "address", "difficulty", "target_type", "stages", "criteria", "prepared_card_template", "initial_assignment")
    ID_FIELD_NUMBER: _ClassVar[int]
    TITLE_FIELD_NUMBER: _ClassVar[int]
    ADDRESS_FIELD_NUMBER: _ClassVar[int]
    DIFFICULTY_FIELD_NUMBER: _ClassVar[int]
    TARGET_TYPE_FIELD_NUMBER: _ClassVar[int]
    STAGES_FIELD_NUMBER: _ClassVar[int]
    CRITERIA_FIELD_NUMBER: _ClassVar[int]
    PREPARED_CARD_TEMPLATE_FIELD_NUMBER: _ClassVar[int]
    INITIAL_ASSIGNMENT_FIELD_NUMBER: _ClassVar[int]
    id: str
    title: str
    address: Address
    difficulty: Difficulty
    target_type: IncidentTargetType
    stages: _containers.RepeatedCompositeFieldContainer[IncidentStage]
    criteria: Criteria
    prepared_card_template: PreparedCardTemplate
    initial_assignment: InitialAssignment
    def __init__(self, id: _Optional[str] = ..., title: _Optional[str] = ..., address: _Optional[_Union[Address, _Mapping]] = ..., difficulty: _Optional[_Union[Difficulty, str]] = ..., target_type: _Optional[_Union[IncidentTargetType, str]] = ..., stages: _Optional[_Iterable[_Union[IncidentStage, _Mapping]]] = ..., criteria: _Optional[_Union[Criteria, _Mapping]] = ..., prepared_card_template: _Optional[_Union[PreparedCardTemplate, _Mapping]] = ..., initial_assignment: _Optional[_Union[InitialAssignment, _Mapping]] = ...) -> None: ...

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
    __slots__ = ("classifier_codes", "victim_count", "position", "expected_routing_facts")
    class ExpectedRoutingFactsEntry(_message.Message):
        __slots__ = ("key", "value")
        KEY_FIELD_NUMBER: _ClassVar[int]
        VALUE_FIELD_NUMBER: _ClassVar[int]
        key: str
        value: str
        def __init__(self, key: _Optional[str] = ..., value: _Optional[str] = ...) -> None: ...
    CLASSIFIER_CODES_FIELD_NUMBER: _ClassVar[int]
    VICTIM_COUNT_FIELD_NUMBER: _ClassVar[int]
    POSITION_FIELD_NUMBER: _ClassVar[int]
    EXPECTED_ROUTING_FACTS_FIELD_NUMBER: _ClassVar[int]
    classifier_codes: _containers.RepeatedScalarFieldContainer[str]
    victim_count: int
    position: int
    expected_routing_facts: _containers.ScalarMap[str, str]
    def __init__(self, classifier_codes: _Optional[_Iterable[str]] = ..., victim_count: _Optional[int] = ..., position: _Optional[int] = ..., expected_routing_facts: _Optional[_Mapping[str, str]] = ...) -> None: ...

class DdsStageDetails(_message.Message):
    __slots__ = ("type", "time_limit_seconds", "expected_comment", "actual_status", "completion_triggers", "fail_on_timeout")
    TYPE_FIELD_NUMBER: _ClassVar[int]
    TIME_LIMIT_SECONDS_FIELD_NUMBER: _ClassVar[int]
    EXPECTED_COMMENT_FIELD_NUMBER: _ClassVar[int]
    ACTUAL_STATUS_FIELD_NUMBER: _ClassVar[int]
    COMPLETION_TRIGGERS_FIELD_NUMBER: _ClassVar[int]
    FAIL_ON_TIMEOUT_FIELD_NUMBER: _ClassVar[int]
    type: DdsStageType
    time_limit_seconds: int
    expected_comment: str
    actual_status: IncidentStatus
    completion_triggers: _containers.RepeatedScalarFieldContainer[DdsCompletionTrigger]
    fail_on_timeout: bool
    def __init__(self, type: _Optional[_Union[DdsStageType, str]] = ..., time_limit_seconds: _Optional[int] = ..., expected_comment: _Optional[str] = ..., actual_status: _Optional[_Union[IncidentStatus, str]] = ..., completion_triggers: _Optional[_Iterable[_Union[DdsCompletionTrigger, str]]] = ..., fail_on_timeout: bool = ...) -> None: ...

class CallScenario(_message.Message):
    __slots__ = ("id", "position", "direction", "counterparty", "person", "gender", "known_facts", "hidden_facts", "ai_context", "emotional_state", "service_code", "incident_address")
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
    SERVICE_CODE_FIELD_NUMBER: _ClassVar[int]
    INCIDENT_ADDRESS_FIELD_NUMBER: _ClassVar[int]
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
    service_code: str
    incident_address: Address
    def __init__(self, id: _Optional[str] = ..., position: _Optional[int] = ..., direction: _Optional[_Union[CallDirection, str]] = ..., counterparty: _Optional[_Union[CounterpartyType, str]] = ..., person: _Optional[_Union[Person, _Mapping]] = ..., gender: _Optional[_Union[Gender, str]] = ..., known_facts: _Optional[_Iterable[str]] = ..., hidden_facts: _Optional[_Iterable[str]] = ..., ai_context: _Optional[str] = ..., emotional_state: _Optional[str] = ..., service_code: _Optional[str] = ..., incident_address: _Optional[_Union[Address, _Mapping]] = ...) -> None: ...

class PreparedCardTemplate(_message.Message):
    __slots__ = ("classifier_codes", "applicant", "victim_count", "additional_info", "assigned_services")
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
    ASSIGNED_SERVICES_FIELD_NUMBER: _ClassVar[int]
    classifier_codes: _containers.RepeatedScalarFieldContainer[str]
    applicant: Person
    victim_count: int
    additional_info: _containers.ScalarMap[str, str]
    assigned_services: _containers.RepeatedScalarFieldContainer[str]
    def __init__(self, classifier_codes: _Optional[_Iterable[str]] = ..., applicant: _Optional[_Union[Person, _Mapping]] = ..., victim_count: _Optional[int] = ..., additional_info: _Optional[_Mapping[str, str]] = ..., assigned_services: _Optional[_Iterable[str]] = ...) -> None: ...

class InitialAssignment(_message.Message):
    __slots__ = ("emergency_service_code",)
    EMERGENCY_SERVICE_CODE_FIELD_NUMBER: _ClassVar[int]
    emergency_service_code: str
    def __init__(self, emergency_service_code: _Optional[str] = ...) -> None: ...

class Criteria(_message.Message):
    __slots__ = ("dialogue_criteria",)
    DIALOGUE_CRITERIA_FIELD_NUMBER: _ClassVar[int]
    dialogue_criteria: _containers.RepeatedCompositeFieldContainer[DialogueCriterion]
    def __init__(self, dialogue_criteria: _Optional[_Iterable[_Union[DialogueCriterion, _Mapping]]] = ...) -> None: ...

class DialogueCriterion(_message.Message):
    __slots__ = ("id", "name", "hypothesis", "weight")
    ID_FIELD_NUMBER: _ClassVar[int]
    NAME_FIELD_NUMBER: _ClassVar[int]
    HYPOTHESIS_FIELD_NUMBER: _ClassVar[int]
    WEIGHT_FIELD_NUMBER: _ClassVar[int]
    id: str
    name: str
    hypothesis: str
    weight: int
    def __init__(self, id: _Optional[str] = ..., name: _Optional[str] = ..., hypothesis: _Optional[str] = ..., weight: _Optional[int] = ...) -> None: ...

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
    __slots__ = ("first_name", "last_name", "middle_name", "age", "phone", "contact_phone", "address", "additional_info", "on_scene_phone")
    FIRST_NAME_FIELD_NUMBER: _ClassVar[int]
    LAST_NAME_FIELD_NUMBER: _ClassVar[int]
    MIDDLE_NAME_FIELD_NUMBER: _ClassVar[int]
    AGE_FIELD_NUMBER: _ClassVar[int]
    PHONE_FIELD_NUMBER: _ClassVar[int]
    CONTACT_PHONE_FIELD_NUMBER: _ClassVar[int]
    ADDRESS_FIELD_NUMBER: _ClassVar[int]
    ADDITIONAL_INFO_FIELD_NUMBER: _ClassVar[int]
    ON_SCENE_PHONE_FIELD_NUMBER: _ClassVar[int]
    first_name: str
    last_name: str
    middle_name: str
    age: int
    phone: str
    contact_phone: str
    address: str
    additional_info: str
    on_scene_phone: str
    def __init__(self, first_name: _Optional[str] = ..., last_name: _Optional[str] = ..., middle_name: _Optional[str] = ..., age: _Optional[int] = ..., phone: _Optional[str] = ..., contact_phone: _Optional[str] = ..., address: _Optional[str] = ..., additional_info: _Optional[str] = ..., on_scene_phone: _Optional[str] = ...) -> None: ...
