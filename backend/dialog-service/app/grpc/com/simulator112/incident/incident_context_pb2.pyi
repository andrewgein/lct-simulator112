from google.protobuf.internal import containers as _containers
from google.protobuf.internal import enum_type_wrapper as _enum_type_wrapper
from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from collections.abc import Iterable as _Iterable, Mapping as _Mapping
from typing import ClassVar as _ClassVar, Optional as _Optional, Union as _Union

DESCRIPTOR: _descriptor.FileDescriptor

class IncidentType(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    FIRE: _ClassVar[IncidentType]
    MEDICAL_EMERGENCY: _ClassVar[IncidentType]
    TRAFFIC_ACCIDENT: _ClassVar[IncidentType]
    CRIME: _ClassVar[IncidentType]
    GAS_LEAK: _ClassVar[IncidentType]
    ACCIDENT: _ClassVar[IncidentType]
    OTHER: _ClassVar[IncidentType]

class ServiceType(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    SERVICE_TYPE_UNSPECIFIED: _ClassVar[ServiceType]
    FIRE_SERVICE: _ClassVar[ServiceType]
    POLICE_SERVICE: _ClassVar[ServiceType]
    AMBULANCE_SERVICE: _ClassVar[ServiceType]
    GAS_SERVICE: _ClassVar[ServiceType]
    ANTI_TERROR_SERVICE: _ClassVar[ServiceType]

class FieldType(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    FIELD_TYPE_UNSPECIFIED: _ClassVar[FieldType]
    TEXT: _ClassVar[FieldType]
    TEXTAREA: _ClassVar[FieldType]
    NUMBER: _ClassVar[FieldType]
    BOOLEAN: _ClassVar[FieldType]

class Difficulty(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    EASY: _ClassVar[Difficulty]
    NORMAL: _ClassVar[Difficulty]
    HARD: _ClassVar[Difficulty]

class EmotionalState(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    CALM: _ClassVar[EmotionalState]
    WORRIED: _ClassVar[EmotionalState]
    PANICKED: _ClassVar[EmotionalState]
    AGGRESSIVE: _ClassVar[EmotionalState]
    CONFUSED: _ClassVar[EmotionalState]

class Gender(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    MAN: _ClassVar[Gender]
    WOMEN: _ClassVar[Gender]
FIRE: IncidentType
MEDICAL_EMERGENCY: IncidentType
TRAFFIC_ACCIDENT: IncidentType
CRIME: IncidentType
GAS_LEAK: IncidentType
ACCIDENT: IncidentType
OTHER: IncidentType
SERVICE_TYPE_UNSPECIFIED: ServiceType
FIRE_SERVICE: ServiceType
POLICE_SERVICE: ServiceType
AMBULANCE_SERVICE: ServiceType
GAS_SERVICE: ServiceType
ANTI_TERROR_SERVICE: ServiceType
FIELD_TYPE_UNSPECIFIED: FieldType
TEXT: FieldType
TEXTAREA: FieldType
NUMBER: FieldType
BOOLEAN: FieldType
EASY: Difficulty
NORMAL: Difficulty
HARD: Difficulty
CALM: EmotionalState
WORRIED: EmotionalState
PANICKED: EmotionalState
AGGRESSIVE: EmotionalState
CONFUSED: EmotionalState
MAN: Gender
WOMEN: Gender

class LevelContext(_message.Message):
    __slots__ = ("id", "title", "incidents", "difficulty")
    ID_FIELD_NUMBER: _ClassVar[int]
    TITLE_FIELD_NUMBER: _ClassVar[int]
    INCIDENTS_FIELD_NUMBER: _ClassVar[int]
    DIFFICULTY_FIELD_NUMBER: _ClassVar[int]
    id: str
    title: str
    incidents: _containers.RepeatedCompositeFieldContainer[IncidentContext]
    difficulty: Difficulty
    def __init__(self, id: _Optional[str] = ..., title: _Optional[str] = ..., incidents: _Optional[_Iterable[_Union[IncidentContext, _Mapping]]] = ..., difficulty: _Optional[_Union[Difficulty, str]] = ...) -> None: ...

class IncidentContext(_message.Message):
    __slots__ = ("id", "title", "address", "dispatcher_criteria", "stages")
    ID_FIELD_NUMBER: _ClassVar[int]
    TITLE_FIELD_NUMBER: _ClassVar[int]
    ADDRESS_FIELD_NUMBER: _ClassVar[int]
    DISPATCHER_CRITERIA_FIELD_NUMBER: _ClassVar[int]
    STAGES_FIELD_NUMBER: _ClassVar[int]
    id: str
    title: str
    address: Address
    dispatcher_criteria: DispatcherCriteria
    stages: _containers.RepeatedCompositeFieldContainer[StageContext]
    def __init__(self, id: _Optional[str] = ..., title: _Optional[str] = ..., address: _Optional[_Union[Address, _Mapping]] = ..., dispatcher_criteria: _Optional[_Union[DispatcherCriteria, _Mapping]] = ..., stages: _Optional[_Iterable[_Union[StageContext, _Mapping]]] = ...) -> None: ...

class StageContext(_message.Message):
    __slots__ = ("id", "title", "position", "dialups", "victim", "additional_info", "description", "type")
    ID_FIELD_NUMBER: _ClassVar[int]
    TITLE_FIELD_NUMBER: _ClassVar[int]
    POSITION_FIELD_NUMBER: _ClassVar[int]
    DIALUPS_FIELD_NUMBER: _ClassVar[int]
    VICTIM_FIELD_NUMBER: _ClassVar[int]
    ADDITIONAL_INFO_FIELD_NUMBER: _ClassVar[int]
    DESCRIPTION_FIELD_NUMBER: _ClassVar[int]
    TYPE_FIELD_NUMBER: _ClassVar[int]
    id: str
    title: str
    position: int
    dialups: _containers.RepeatedCompositeFieldContainer[DialupContext]
    victim: Applicant
    additional_info: _containers.RepeatedCompositeFieldContainer[IncidentAdditionalInfo]
    description: str
    type: IncidentTypeInfo
    def __init__(self, id: _Optional[str] = ..., title: _Optional[str] = ..., position: _Optional[int] = ..., dialups: _Optional[_Iterable[_Union[DialupContext, _Mapping]]] = ..., victim: _Optional[_Union[Applicant, _Mapping]] = ..., additional_info: _Optional[_Iterable[_Union[IncidentAdditionalInfo, _Mapping]]] = ..., description: _Optional[str] = ..., type: _Optional[_Union[IncidentTypeInfo, _Mapping]] = ...) -> None: ...

class DialupContext(_message.Message):
    __slots__ = ("id", "position", "dialup_details", "applicant", "victim")
    ID_FIELD_NUMBER: _ClassVar[int]
    POSITION_FIELD_NUMBER: _ClassVar[int]
    DIALUP_DETAILS_FIELD_NUMBER: _ClassVar[int]
    APPLICANT_FIELD_NUMBER: _ClassVar[int]
    VICTIM_FIELD_NUMBER: _ClassVar[int]
    id: str
    position: int
    dialup_details: DialupDetails
    applicant: Applicant
    victim: Applicant
    def __init__(self, id: _Optional[str] = ..., position: _Optional[int] = ..., dialup_details: _Optional[_Union[DialupDetails, _Mapping]] = ..., applicant: _Optional[_Union[Applicant, _Mapping]] = ..., victim: _Optional[_Union[Applicant, _Mapping]] = ...) -> None: ...

class IncidentTypeInfo(_message.Message):
    __slots__ = ("id", "type_id", "service_type", "type_name")
    ID_FIELD_NUMBER: _ClassVar[int]
    TYPE_ID_FIELD_NUMBER: _ClassVar[int]
    SERVICE_TYPE_FIELD_NUMBER: _ClassVar[int]
    TYPE_NAME_FIELD_NUMBER: _ClassVar[int]
    id: str
    type_id: str
    service_type: ServiceType
    type_name: str
    def __init__(self, id: _Optional[str] = ..., type_id: _Optional[str] = ..., service_type: _Optional[_Union[ServiceType, str]] = ..., type_name: _Optional[str] = ...) -> None: ...

class IncidentAdditionalInfo(_message.Message):
    __slots__ = ("id", "additional_info_id", "field_code", "field_name", "field_type", "required", "field_value")
    ID_FIELD_NUMBER: _ClassVar[int]
    ADDITIONAL_INFO_ID_FIELD_NUMBER: _ClassVar[int]
    FIELD_CODE_FIELD_NUMBER: _ClassVar[int]
    FIELD_NAME_FIELD_NUMBER: _ClassVar[int]
    FIELD_TYPE_FIELD_NUMBER: _ClassVar[int]
    REQUIRED_FIELD_NUMBER: _ClassVar[int]
    FIELD_VALUE_FIELD_NUMBER: _ClassVar[int]
    id: str
    additional_info_id: str
    field_code: str
    field_name: str
    field_type: FieldType
    required: bool
    field_value: str
    def __init__(self, id: _Optional[str] = ..., additional_info_id: _Optional[str] = ..., field_code: _Optional[str] = ..., field_name: _Optional[str] = ..., field_type: _Optional[_Union[FieldType, str]] = ..., required: _Optional[bool] = ..., field_value: _Optional[str] = ...) -> None: ...

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

class Applicant(_message.Message):
    __slots__ = ("first_name", "last_name", "middle_name", "age", "phone", "emotional_state", "contact_phone", "address", "additional_info")
    FIRST_NAME_FIELD_NUMBER: _ClassVar[int]
    LAST_NAME_FIELD_NUMBER: _ClassVar[int]
    MIDDLE_NAME_FIELD_NUMBER: _ClassVar[int]
    AGE_FIELD_NUMBER: _ClassVar[int]
    PHONE_FIELD_NUMBER: _ClassVar[int]
    EMOTIONAL_STATE_FIELD_NUMBER: _ClassVar[int]
    CONTACT_PHONE_FIELD_NUMBER: _ClassVar[int]
    ADDRESS_FIELD_NUMBER: _ClassVar[int]
    ADDITIONAL_INFO_FIELD_NUMBER: _ClassVar[int]
    first_name: str
    last_name: str
    middle_name: str
    age: int
    phone: str
    emotional_state: EmotionalState
    contact_phone: str
    address: str
    additional_info: str
    def __init__(self, first_name: _Optional[str] = ..., last_name: _Optional[str] = ..., middle_name: _Optional[str] = ..., age: _Optional[int] = ..., phone: _Optional[str] = ..., emotional_state: _Optional[_Union[EmotionalState, str]] = ..., contact_phone: _Optional[str] = ..., address: _Optional[str] = ..., additional_info: _Optional[str] = ...) -> None: ...

class DialupDetails(_message.Message):
    __slots__ = ("gender", "known_facts", "hidden_facts", "ai_context", "emotional_state")
    GENDER_FIELD_NUMBER: _ClassVar[int]
    KNOWN_FACTS_FIELD_NUMBER: _ClassVar[int]
    HIDDEN_FACTS_FIELD_NUMBER: _ClassVar[int]
    AI_CONTEXT_FIELD_NUMBER: _ClassVar[int]
    EMOTIONAL_STATE_FIELD_NUMBER: _ClassVar[int]
    gender: Gender
    known_facts: _containers.RepeatedScalarFieldContainer[str]
    hidden_facts: _containers.RepeatedScalarFieldContainer[str]
    ai_context: str
    emotional_state: str
    def __init__(self, gender: _Optional[_Union[Gender, str]] = ..., known_facts: _Optional[_Iterable[str]] = ..., hidden_facts: _Optional[_Iterable[str]] = ..., ai_context: _Optional[str] = ..., emotional_state: _Optional[str] = ...) -> None: ...

class DispatcherCriteria(_message.Message):
    __slots__ = ("required_questions", "expected_actions", "critical_mistakes")
    REQUIRED_QUESTIONS_FIELD_NUMBER: _ClassVar[int]
    EXPECTED_ACTIONS_FIELD_NUMBER: _ClassVar[int]
    CRITICAL_MISTAKES_FIELD_NUMBER: _ClassVar[int]
    required_questions: _containers.RepeatedScalarFieldContainer[str]
    expected_actions: _containers.RepeatedScalarFieldContainer[str]
    critical_mistakes: _containers.RepeatedScalarFieldContainer[str]
    def __init__(self, required_questions: _Optional[_Iterable[str]] = ..., expected_actions: _Optional[_Iterable[str]] = ..., critical_mistakes: _Optional[_Iterable[str]] = ...) -> None: ...
