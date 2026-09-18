from app.grpc.com.simulator112.incident import incident_context_pb2 as _incident_context_pb2
from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from typing import ClassVar as _ClassVar, Optional as _Optional

DESCRIPTOR: _descriptor.FileDescriptor

class GetIncidentContextRequest(_message.Message):
    __slots__ = ("level_id",)
    LEVEL_ID_FIELD_NUMBER: _ClassVar[int]
    level_id: str
    def __init__(self, level_id: _Optional[str] = ...) -> None: ...
