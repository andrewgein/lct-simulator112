# Generated-client equivalent for context_service.proto.
import grpc
from google.protobuf import empty_pb2

from app.grpc.com.simulator112.context import context_service_pb2 as context_pb
from app.grpc.com.simulator112.incident import incident_context_pb2 as incident_pb


class ContextManagerServiceStub:
    def __init__(self, channel: grpc.Channel):
        unary = channel.unary_unary
        self.GetIncidentContext = unary('/context.ContextManagerService/GetIncidentContext', request_serializer=context_pb.GetIncidentContextRequest.SerializeToString, response_deserializer=incident_pb.IncidentContext.FromString)
        self.AppendDialogContext = unary('/context.ContextManagerService/AppendDialogContext', request_serializer=context_pb.AppendDialogContextRequest.SerializeToString, response_deserializer=empty_pb2.Empty.FromString)
        self.AppendSolutionContext = unary('/context.ContextManagerService/AppendSolutionContext', request_serializer=context_pb.AppendSolutionContextRequest.SerializeToString, response_deserializer=empty_pb2.Empty.FromString)
        self.GetFullContext = unary('/context.ContextManagerService/GetFullContext', request_serializer=context_pb.GetFullContextRequest.SerializeToString, response_deserializer=context_pb.FullContext.FromString)
        self.GetNextCall = unary('/context.ContextManagerService/GetNextCall', request_serializer=context_pb.GetNextCallRequest.SerializeToString, response_deserializer=incident_pb.CallScenario.FromString)
        self.GetCall = unary('/context.ContextManagerService/GetCall', request_serializer=context_pb.GetCallRequest.SerializeToString, response_deserializer=incident_pb.CallScenario.FromString)
        self.GetDialogProgress = unary('/context.ContextManagerService/GetDialogProgress', request_serializer=context_pb.GetDialogProgressRequest.SerializeToString, response_deserializer=context_pb.DialogProgress.FromString)
        self.StartCall = unary('/context.ContextManagerService/StartCall', request_serializer=context_pb.StartCallRequest.SerializeToString, response_deserializer=context_pb.DialogProgress.FromString)
        self.CompleteCall = unary('/context.ContextManagerService/CompleteCall', request_serializer=context_pb.CompleteCallRequest.SerializeToString, response_deserializer=context_pb.DialogProgress.FromString)
        self.DisconnectCall = unary('/context.ContextManagerService/DisconnectCall', request_serializer=context_pb.DisconnectCallRequest.SerializeToString, response_deserializer=context_pb.DialogProgress.FromString)
        self.GetLevelProgress = unary('/context.ContextManagerService/GetLevelProgress', request_serializer=context_pb.GetLevelProgressRequest.SerializeToString, response_deserializer=context_pb.LevelProgress.FromString)
        self.ApplyDdsStageSignal = unary('/context.ContextManagerService/ApplyDdsStageSignal', request_serializer=context_pb.ApplyDdsStageSignalRequest.SerializeToString, response_deserializer=context_pb.LevelProgress.FromString)
