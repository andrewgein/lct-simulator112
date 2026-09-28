from app.domain.model import (
    CallDirection,
    CallScenario,
    CounterpartyType,
    DialogProgress,
    DialogStatus,
    DialogTranscript,
    Gender,
    Person,
    Speaker,
)
from app.grpc.com.simulator112.context import context_service_pb2 as context_pb
from app.grpc.com.simulator112.incident import incident_context_pb2 as incident_pb


def call_from_proto(value: incident_pb.CallScenario) -> CallScenario:
    person = value.person
    return CallScenario(
        id=value.id,
        position=value.position,
        direction={
            incident_pb.CALL_DIRECTION_INBOUND: CallDirection.INBOUND,
            incident_pb.CALL_DIRECTION_OUTBOUND: CallDirection.OUTBOUND,
        }.get(value.direction, CallDirection.UNSPECIFIED),
        counterparty={
            incident_pb.COUNTERPARTY_TYPE_CALLER: CounterpartyType.CALLER,
            incident_pb.COUNTERPARTY_TYPE_BRIGADE: CounterpartyType.BRIGADE,
            incident_pb.COUNTERPARTY_TYPE_SERVICE: CounterpartyType.SERVICE,
        }.get(value.counterparty, CounterpartyType.UNSPECIFIED),
        person=Person(
            first_name=person.first_name,
            last_name=person.last_name,
            middle_name=person.middle_name,
            age=person.age if person.HasField("age") else None,
            phone=person.phone,
            contact_phone=person.contact_phone,
            address=person.address,
            additional_info=person.additional_info,
        ),
        gender={
            incident_pb.GENDER_MAN: Gender.MAN,
            incident_pb.GENDER_WOMEN: Gender.WOMEN,
        }.get(value.gender, Gender.UNSPECIFIED),
        known_facts=tuple(value.known_facts),
        hidden_facts=tuple(value.hidden_facts),
        ai_context=value.ai_context,
        emotional_state=value.emotional_state,
        service_code=value.service_code,
    )


def progress_from_proto(value: context_pb.DialogProgress) -> DialogProgress:
    status = {
        context_pb.IDLE: DialogStatus.IDLE,
        context_pb.IN_CALL: DialogStatus.IN_CALL,
        context_pb.DISCONNECTED: DialogStatus.DISCONNECTED,
        context_pb.COMPLETED: DialogStatus.COMPLETED,
    }.get(value.status)
    if status is None:
        raise ValueError(f"Неизвестный статус диалога: {value.status}")
    return DialogProgress(value.context_id, value.active_call_id, status)


def transcript_to_proto(value: DialogTranscript) -> context_pb.DialogContext:
    speakers = {Speaker.OPERATOR: context_pb.USER, Speaker.COUNTERPARTY: context_pb.LLM}
    return context_pb.DialogContext(transcript=[
        context_pb.Phrase(speaker=speakers[phrase.speaker], text=phrase.text)
        for phrase in value.phrases
    ])
