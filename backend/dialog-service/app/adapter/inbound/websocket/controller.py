import asyncio
from contextlib import suppress
import logging
import threading

from fastapi import APIRouter, WebSocket, WebSocketDisconnect, status

from app.application.port.inbound import DialogUseCase
from app.application.port.outbound import (
    CallRecorderFactory,
    NoMoreCallsError,
    VoicePipelineFactory,
)
from app.adapter.out.processing.latency_tracker import tracker
from app.application.service.transcript_builder import DialogContextBuilder
from app.domain.model import DialogProgress, DialogStatus

SERVICE_LOAD_PUSH_INTERVAL_SECONDS = 5

logger = logging.getLogger(__name__)
router = APIRouter()
MAX_AUDIO_FRAME_BYTES = 32 * 1024
_dialog_use_case: DialogUseCase | None = None
_voice_pipeline_factory: VoicePipelineFactory | None = None
_call_recorder_factory: CallRecorderFactory | None = None


def configure(
    dialog: DialogUseCase,
    voice_pipeline: VoicePipelineFactory,
    call_recorder: CallRecorderFactory,
) -> None:
    global _dialog_use_case, _voice_pipeline_factory, _call_recorder_factory
    _dialog_use_case = dialog
    _voice_pipeline_factory = voice_pipeline
    _call_recorder_factory = call_recorder


def dialog_use_case() -> DialogUseCase:
    if _dialog_use_case is None:
        raise RuntimeError("WebSocket adapter не сконфигурирован")
    return _dialog_use_case


def voice_pipeline_factory() -> VoicePipelineFactory:
    if _voice_pipeline_factory is None:
        raise RuntimeError("WebSocket adapter не сконфигурирован")
    return _voice_pipeline_factory


def call_recorder_factory() -> CallRecorderFactory:
    if _call_recorder_factory is None:
        raise RuntimeError("Call recorder adapter не сконфигурирован")
    return _call_recorder_factory


def is_valid_context_id(context_id: object) -> bool:
    return isinstance(context_id, str) and context_id.strip().lower() not in {"", "null", "none"}


def is_call_active(progress: DialogProgress):
    return progress.status in (DialogStatus.IN_CALL, DialogStatus.DISCONNECTED)

def was_call_disconnected(progress: DialogProgress):
    return progress.status == DialogStatus.DISCONNECTED


def handle_progress_request(context_id: str) -> dict:
    session = dialog_use_case().session(context_id)
    progress = session.progress
    if is_call_active(progress) and session.call is not None:
        call = session.call
        return {
            "type": "session_restored",
            "callAvailable": True,
            "callId": progress.active_call_id,
            "phoneNumber": call.person.phone,
            "interrupted": was_call_disconnected(progress),
        }
    if progress.status == DialogStatus.COMPLETED:
        return {
            "type": "call_finished",
            "cardCanBeEdited": True,
            "nextCallAvailable": True,
            "callId": progress.active_call_id,
        }
    return {"type": "idle"}


def handle_next_call(context_id: str) -> dict:
    session = dialog_use_case().session(context_id)
    progress = session.progress
    if is_call_active(progress) and session.call is not None:
        call = session.call
        return {
            "type": "session_restored",
            "callAvailable": True,
            "callId": progress.active_call_id,
            "phoneNumber": call.person.phone,
            "interrupted": was_call_disconnected(progress),
        }

    try:
        call = dialog_use_case().next_call(context_id)
    except NoMoreCallsError:
        return {"type": "no_more_calls"}
    return {
        "type": "call_ready",
        "callAvailable": True,
        "callId": call.id,
        "phoneNumber": call.person.phone,
    }


def handle_error(message: str) -> dict:
    return {"type": "error", "message": message}


async def _push_service_load(ws: WebSocket, send_lock: asyncio.Lock) -> None:
    while True:
        await asyncio.sleep(SERVICE_LOAD_PUSH_INTERVAL_SECONDS)
        async with send_lock:
            await ws.send_json({"type": "service_load", **tracker.snapshot()})


@router.websocket("/api/v1/dialog/session")
async def dialog_session(ws: WebSocket):
    await ws.accept()
    context_id = ws.query_params.get("contextId")
    if context_id is None or not is_valid_context_id(context_id):
        await ws.close(code=status.WS_1008_POLICY_VIOLATION, reason="No contextId provided")
        return

    send_lock = asyncio.Lock()
    push_task = asyncio.create_task(_push_service_load(ws, send_lock))
    try:
        async with send_lock:
            await ws.send_json(await asyncio.to_thread(handle_progress_request, context_id))
        while True:
            request = await ws.receive_json()
            request_type = request.get("type")

            match request_type:
                case "request_status":
                    response = await asyncio.to_thread(handle_progress_request, context_id)
                case "request_next_call":
                    response = await asyncio.to_thread(handle_next_call, context_id)
                case _:
                    response = handle_error("Unknown session command")
            async with send_lock:
                await ws.send_json(response)

    except WebSocketDisconnect:
        logger.info("Control connection closed for %s", context_id)
    except Exception as exc:
        logger.exception("Dialog control session failed for %s", context_id)
        try:
            async with send_lock:
                await ws.send_json(handle_error(str(exc)))
        except (WebSocketDisconnect, RuntimeError):
            pass
    finally:
        push_task.cancel()
        with suppress(asyncio.CancelledError, WebSocketDisconnect, RuntimeError):
            await push_task


@router.websocket("/api/v1/dialog/process-call")
async def process_call(ws: WebSocket):
    await ws.accept()
    context_id = ws.query_params.get("contextId")
    if not is_valid_context_id(context_id):
        await ws.close(code=status.WS_1008_POLICY_VIOLATION, reason="No contextId provided")
        return

    restart = ws.query_params.get("restart", "").lower() == "true"
    processing_context = None
    call_recorder = None
    call_id = None
    completion_requested = False
    dialog_context_builder = DialogContextBuilder()
    loop = asyncio.get_running_loop()
    client_disconnected = threading.Event()

    async def send_audio_chunk(audio_chunk: bytes):
        try:
            for offset in range(0, len(audio_chunk), MAX_AUDIO_FRAME_BYTES):
                if client_disconnected.is_set():
                    return
                await ws.send_bytes(audio_chunk[offset:offset + MAX_AUDIO_FRAME_BYTES])
        except (WebSocketDisconnect, RuntimeError):
            client_disconnected.set()

    def output_callback(pcm: bytes) -> None:
        if not client_disconnected.is_set():
            call_recorder.record_counterparty(pcm)
            asyncio.run_coroutine_threadsafe(send_audio_chunk(pcm), loop).result()

    try:
        session = await asyncio.to_thread(dialog_use_case().session, context_id)
        progress = session.progress
        if not is_call_active(progress) or not progress.active_call_id:
            await ws.close(code=status.WS_1008_POLICY_VIOLATION, reason="No active call")
            return
        call_id = progress.active_call_id
        history = None
        if restart:
            call = await asyncio.to_thread(dialog_use_case().restart_call, context_id)
        else:
            call = await asyncio.to_thread(dialog_use_case().resume_call, context_id)
            if was_call_disconnected(progress):
                history = await asyncio.to_thread(
                    dialog_use_case().transcript_for_resume, context_id, call_id)
        call_recorder = await asyncio.to_thread(call_recorder_factory().create, context_id, call_id)
        # Retain ownership if the handler is cancelled during construction, so
        # the factory's newly started workers are still closed by finally.
        creation = asyncio.create_task(asyncio.to_thread(
            voice_pipeline_factory().create,
            call=call,
            on_operator_phrase=dialog_context_builder.append_user_phrase,
            on_counterparty_phrase=dialog_context_builder.append_llm_phrase,
            on_audio=output_callback,
            history=history,
        ))
        try:
            processing_context = await asyncio.shield(creation)
        except asyncio.CancelledError:
            processing_context = await creation
            raise

        while True:
            message = await ws.receive()
            if message.get("type") == "websocket.disconnect":
                break
            if message.get("text") is not None:
                if message["text"] == "end_call":
                    completion_requested = True
                    break
                processing_context.process_text(message["text"])
            elif message.get("bytes") is not None:
                call_recorder.record_operator(message["bytes"])
                processing_context.process_audio(message["bytes"])
    except WebSocketDisconnect:
        pass
    finally:
        client_disconnected.set()
        try:
            if processing_context is not None:
                await asyncio.to_thread(processing_context.close)
        finally:
            try:
                if call_recorder is not None:
                    await asyncio.to_thread(call_recorder.close)
            except Exception:
                logger.exception("Could not save call recording for %s (call %s)", context_id, call_id)
            finally:
                if call_id is not None:
                    # No worker can append a phrase after this snapshot.
                    transcript = dialog_context_builder.get()
                    persist = dialog_use_case().complete if completion_requested else dialog_use_case().disconnect
                    try:
                        await asyncio.to_thread(persist, context_id, call_id, transcript)
                    except Exception:
                        logger.exception("Could not persist call %s", call_id)
                        with suppress(WebSocketDisconnect, RuntimeError):
                            await ws.close(code=status.WS_1011_INTERNAL_ERROR)
                        raise
                if completion_requested:
                    with suppress(WebSocketDisconnect, RuntimeError):
                        await ws.close(code=status.WS_1000_NORMAL_CLOSURE)
