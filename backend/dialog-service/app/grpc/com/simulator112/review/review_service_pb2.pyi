from app.grpc.com.simulator112.context import context_service_pb2 as _context_service_pb2
from google.protobuf.internal import enum_type_wrapper as _enum_type_wrapper
from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from collections.abc import Mapping as _Mapping
from typing import ClassVar as _ClassVar, Optional as _Optional, Union as _Union

DESCRIPTOR: _descriptor.FileDescriptor

class ReviewStatus(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    CREATED: _ClassVar[ReviewStatus]
    FILLED: _ClassVar[ReviewStatus]
    IN_REVIEW: _ClassVar[ReviewStatus]
    DIALOG: _ClassVar[ReviewStatus]
    DONE: _ClassVar[ReviewStatus]
CREATED: ReviewStatus
FILLED: ReviewStatus
IN_REVIEW: ReviewStatus
DIALOG: ReviewStatus
DONE: ReviewStatus

class SendOnReviewRequest(_message.Message):
    __slots__ = ("context",)
    CONTEXT_FIELD_NUMBER: _ClassVar[int]
    context: _context_service_pb2.FullContext
    def __init__(self, context: _Optional[_Union[_context_service_pb2.FullContext, _Mapping]] = ...) -> None: ...

class GetReviewRequest(_message.Message):
    __slots__ = ("uuid",)
    UUID_FIELD_NUMBER: _ClassVar[int]
    uuid: str
    def __init__(self, uuid: _Optional[str] = ...) -> None: ...

class ReviewStatusResponse(_message.Message):
    __slots__ = ("status",)
    STATUS_FIELD_NUMBER: _ClassVar[int]
    status: ReviewStatus
    def __init__(self, status: _Optional[_Union[ReviewStatus, str]] = ...) -> None: ...
