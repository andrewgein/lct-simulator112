import os
from dataclasses import dataclass
from functools import lru_cache

from app.adapter.out.grpc.context_adapter import GrpcContextAdapter
from app.adapter.out.mock.context_adapter import MockContextAdapter
from app.adapter.out.processing.voice_pipeline_adapter import ProcessingVoicePipelineFactory
from app.adapter.out.recording import S3CallRecorderFactory
from app.application.port.inbound import DialogUseCase
from app.application.port.outbound import CallRecorderFactory, VoicePipelineFactory
from app.application.service.dialog_service import DialogService


@dataclass(frozen=True)
class ApplicationComponents:
    dialog: DialogUseCase
    voice_pipeline: VoicePipelineFactory
    call_recorder: CallRecorderFactory


@lru_cache(maxsize=1)
def components() -> ApplicationComponents:
    source = os.getenv("CONTEXT_SOURCE", "grpc").strip().lower()
    if source == "grpc":
        context_port = GrpcContextAdapter()
    elif source == "mock":
        context_port = MockContextAdapter()
    else:
        raise ValueError("CONTEXT_SOURCE должен иметь значение mock или grpc")
    return ApplicationComponents(
        dialog=DialogService(context_port),
        voice_pipeline=ProcessingVoicePipelineFactory(),
        call_recorder=S3CallRecorderFactory(),
    )
