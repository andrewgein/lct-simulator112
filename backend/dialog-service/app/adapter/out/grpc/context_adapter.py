import os

import grpc

from app.adapter.out.grpc.context_mapper import call_from_proto, progress_from_proto, transcript_to_proto
from app.application.port.outbound import ContextPort, NoMoreCallsError
from app.domain.model import CallScenario, DialogProgress, DialogTranscript
from app.grpc.com.simulator112.context import context_service_pb2 as context_pb
from app.grpc.com.simulator112.context.context_service_pb2_grpc import ContextManagerServiceStub


class GrpcContextAdapter(ContextPort):
    def __init__(self, address: str | None = None, timeout: float = 10):
        self._address = address or os.getenv("CONTEXT_MANAGER_GRPC_URL", "127.0.0.1:9090")
        self._timeout = timeout

    def _call(self, method: str, request):
        try:
            with grpc.insecure_channel(self._address) as channel:
                return getattr(ContextManagerServiceStub(channel), method)(request, timeout=self._timeout)
        except grpc.RpcError as exc:
            if exc.code() == grpc.StatusCode.NOT_FOUND and method == "GetNextCall":
                raise NoMoreCallsError("Доступные звонки закончились") from exc
            raise

    def get_progress(self, context_id: str) -> DialogProgress:
        return progress_from_proto(self._call("GetDialogProgress", context_pb.GetDialogProgressRequest(context_id=context_id)))

    def get_next_call(self, context_id: str, current_call_id: str = "-1") -> CallScenario:
        value = self._call("GetNextCall", context_pb.GetNextCallRequest(uuid=context_id, current_call_id=current_call_id))
        return call_from_proto(value)

    def get_call(self, context_id: str, call_id: str) -> CallScenario:
        return call_from_proto(self._call("GetCall", context_pb.GetCallRequest(context_id=context_id, call_id=call_id)))

    def start_call(self, context_id: str, call_id: str) -> DialogProgress:
        return progress_from_proto(self._call("StartCall", context_pb.StartCallRequest(context_id=context_id, call_id=call_id)))

    def complete_call(self, context_id: str, call_id: str) -> DialogProgress:
        return progress_from_proto(self._call("CompleteCall", context_pb.CompleteCallRequest(context_id=context_id, call_id=call_id)))

    def disconnect_call(self, context_id: str, call_id: str) -> DialogProgress:
        return progress_from_proto(self._call("DisconnectCall", context_pb.DisconnectCallRequest(context_id=context_id, call_id=call_id)))

    def append_transcript(self, context_id: str, transcript: DialogTranscript) -> None:
        self._call("AppendDialogContext", context_pb.AppendDialogContextRequest(
            uuid=context_id, dialog_context=transcript_to_proto(transcript)))
