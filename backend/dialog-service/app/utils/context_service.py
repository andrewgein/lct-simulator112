import os

import grpc

from app.grpc.com.simulator112.context.context_service_pb2 import (
    AppendDialogContextRequest,
    CompleteDialupRequest,
    DialogContext,
    DisconnectDialupRequest,
    GetDialogProgressRequest,
    GetDialupRequest,
    GetNextDialupRequest,
    StartDialupRequest,
)
from app.grpc.com.simulator112.context.context_service_pb2_grpc import ContextManagerServiceStub
from app.grpc.com.simulator112.incident.incident_context_pb2 import DialupContext


def is_valid_context_id(context_id) -> bool:
    return isinstance(context_id, str) and context_id.strip().lower() not in {"", "null", "none"}


def _source() -> str:
    source = os.getenv("CONTEXT_SOURCE", "grpc").strip().lower()
    if source not in {"mock", "grpc"}:
        raise ValueError("CONTEXT_SOURCE must be mock or grpc")
    return source


def _address() -> str:
    return os.getenv("CONTEXT_MANAGER_GRPC_URL") or os.getenv(
        "CONTEXT_MANGER_GRPC_URL", "127.0.0.1:9090"
    )


def _call(method_name: str, request):
    if _source() == "mock":
        from app.utils.mock_context_service import call
        return call(method_name, request)
    with grpc.insecure_channel(_address()) as channel:
        return getattr(ContextManagerServiceStub(channel), method_name)(request, timeout=10)


def get_next_dialup(context_id: str, current_dialup_id: str = "-1") -> DialupContext:
    return _call(
        "GetNextDialup",
        GetNextDialupRequest(uuid=context_id, current_dialup_id=current_dialup_id),
    )


def get_dialog_progress(context_id: str):
    return _call("GetDialogProgress", GetDialogProgressRequest(context_id=context_id))


def start_dialup(context_id: str, dialup_id: str):
    return _call("StartDialup", StartDialupRequest(context_id=context_id, dialup_id=dialup_id))


def complete_dialup(context_id: str, dialup_id: str):
    return _call("CompleteDialup", CompleteDialupRequest(context_id=context_id, dialup_id=dialup_id))


def mark_dialog_disconnected(context_id: str, dialup_id: str):
    return _call(
        "DisconnectDialup",
        DisconnectDialupRequest(context_id=context_id, dialup_id=dialup_id),
    )


def get_dialup(context_id: str, dialup_id: str) -> DialupContext:
    return _call("GetDialup", GetDialupRequest(context_id=context_id, dialup_id=dialup_id))


def send_dialog_context(context_id: str, dialog_context: DialogContext):
    _call(
        "AppendDialogContext",
        AppendDialogContextRequest(uuid=context_id, dialog_context=dialog_context),
    )
