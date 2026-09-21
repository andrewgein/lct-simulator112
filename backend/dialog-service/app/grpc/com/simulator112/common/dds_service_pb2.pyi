from google.protobuf.internal import enum_type_wrapper as _enum_type_wrapper
from google.protobuf import descriptor as _descriptor
from typing import ClassVar as _ClassVar

DESCRIPTOR: _descriptor.FileDescriptor

class DdsService(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    DDS_SERVICE_UNSPECIFIED: _ClassVar[DdsService]
    DDS_SERVICE_FIRE: _ClassVar[DdsService]
    DDS_SERVICE_POLICE: _ClassVar[DdsService]
    DDS_SERVICE_AMBULANCE: _ClassVar[DdsService]
    DDS_SERVICE_GAS: _ClassVar[DdsService]
    DDS_SERVICE_ANTI_TERROR: _ClassVar[DdsService]
DDS_SERVICE_UNSPECIFIED: DdsService
DDS_SERVICE_FIRE: DdsService
DDS_SERVICE_POLICE: DdsService
DDS_SERVICE_AMBULANCE: DdsService
DDS_SERVICE_GAS: DdsService
DDS_SERVICE_ANTI_TERROR: DdsService
