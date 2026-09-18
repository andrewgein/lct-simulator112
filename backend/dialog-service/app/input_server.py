import asyncio
import logging
import threading

import grpc
import numpy as np
from fastapi import APIRouter, WebSocket, WebSocketDisconnect, status

from app.grpc.com.simulator112.context.context_service_pb2 import DialogProgress, DialogProgressStatus
from app.processing.chat_node import ChatNode
from app.processing.output_node import OutputNode
from app.processing.processing_context import UserDialogProcessingContext
from app.processing.sst_node import SSTNode
from app.processing.tts_node import TTSNode
from app.utils.context_service import (
    complete_dialup,
    get_dialog_progress,
    get_dialup,
    get_next_dialup,
    is_valid_context_id,
    mark_dialog_disconnected,
    send_dialog_context,
    start_dialup,
)
from app.utils.dialog_context_builder import DialogContextBuilder

logger = logging.getLogger(__name__)
np.set_printoptions(threshold=10000)
router = APIRouter()
MAX_AUDIO_FRAME_BYTES = 32 * 1024

def is_call_active(progress: DialogProgress):
    return progress.status in (DialogProgressStatus.IN_CALL, DialogProgressStatus.DISCONNECTED)

def was_call_disconnected(progress: DialogProgress):
    return progress.status == DialogProgressStatus.DISCONNECTED


def handle_progress_request(context_id: str) -> dict:
    progress = get_dialog_progress(context_id)
    if is_call_active(progress) and progress.active_dialup_id:
        dialup = get_dialup(context_id, progress.active_dialup_id)
        return {
            "type": "session_restored",
            "callAvailable": True,
            "dialupId": progress.active_dialup_id,
            "phoneNumber": dialup.applicant.phone,
        }
    if progress.status == DialogProgressStatus.COMPLETED:
        return {
            "type": "call_finished",
            "cardCanBeEdited": True,
            "nextDialupAvailable": True,
            "dialupId": progress.active_dialup_id,
        }
    return {"type": "idle"}


def handle_next_dialup(context_id: str) -> dict:
    progress = get_dialog_progress(context_id)
    if is_call_active(progress):
        dialup = get_dialup(context_id, progress.active_dialup_id)
        return {
            "type": "session_restored",
            "callAvailable": True,
            "dialupId": progress.active_dialup_id,
            "phoneNumber": dialup.applicant.phone,
        }

    previous_id = progress.active_dialup_id if progress.status == DialogProgressStatus.COMPLETED else "-1"
    try:
        dialup = get_next_dialup(context_id, previous_id)
    except grpc.RpcError as exc:
        if exc.code() == grpc.StatusCode.NOT_FOUND:
            return {"type": "no_more_dialups"}
        raise
    start_dialup(context_id, dialup.id)
    return {
        "type": "dialup_ready",
        "callAvailable": True,
        "dialupId": dialup.id,
        "phoneNumber": dialup.applicant.phone,
    }


def handle_error(message: str) -> dict:
    return {"type": "error", "message": message}


@router.websocket("/api/v1/dialog/session")
async def dialog_session(ws: WebSocket):
    await ws.accept()
    context_id = ws.query_params.get("contextId")
    if context_id == None or not is_valid_context_id(context_id):
        await ws.close(code=status.WS_1008_POLICY_VIOLATION, reason="No contextId provided")
        return

    try:
        await ws.send_json(handle_progress_request(context_id))
        while True:
            request = await ws.receive_json()
            request_type = request.get("type")

            match request_type:
                case "request_status":
                    await ws.send_json(handle_progress_request(context_id))
                case "request_next_dialup":
                    await ws.send_json(handle_next_dialup(context_id))
                case _:
                    await ws.send_json(handle_error("Unknown session command"))

    except WebSocketDisconnect:
        logger.info("Control connection closed for %s", context_id)
    except Exception as exc:
        logger.exception("Dialog control session failed for %s", context_id)
        try:
            await ws.send_json(handle_error(str(exc)))
        except WebSocketDisconnect:
            pass


@router.websocket("/api/v1/dialog/process-call")
async def process_call(ws: WebSocket):
    await ws.accept()
    logger.info("New call connection")
    context_id = ws.query_params.get("contextId")
    if context_id is None or not is_valid_context_id(context_id):
        logger.warning("Call connection rejected: no valid contextId provided")
        await ws.close(code=status.WS_1008_POLICY_VIOLATION, reason="No contextId provided")
        return

    logger.info("Call contextId: %s", context_id)
    processing_context = None
    dialog_context_builder = DialogContextBuilder()
    completed = False
    progress = get_dialog_progress(context_id)
    if not is_call_active(progress) or not progress.active_dialup_id:
        logger.info("Closing process-call for %s: no active dialup", context_id)
        await ws.close(code=status.WS_1008_POLICY_VIOLATION, reason="No active dialup")
        return
    dialup_id = progress.active_dialup_id
    if was_call_disconnected(progress):
        start_dialup(context_id, dialup_id)
    dialup = get_dialup(context_id, dialup_id)

    loop = asyncio.get_running_loop()
    client_disconnected = threading.Event()

    async def send_audio_chunk(audio_chunk: bytes):
        if client_disconnected.is_set():
            return
        try:
            for offset in range(0, len(audio_chunk), MAX_AUDIO_FRAME_BYTES):
                await ws.send_bytes(audio_chunk[offset:offset + MAX_AUDIO_FRAME_BYTES])
        except (WebSocketDisconnect, RuntimeError):
            # The proxy/client can close while TTS is still producing audio. Stop forwarding
            # immediately; pipeline cleanup below will stop the worker threads.
            client_disconnected.set()

    def output_callback(audio_chunk):
        if client_disconnected.is_set():
            return
        audio_chunk = audio_chunk * 32767.0
        np.clip(audio_chunk, -32768, 32767, out=audio_chunk)
        pcm = audio_chunk.astype(np.int16).tobytes()
        asyncio.run_coroutine_threadsafe(send_audio_chunk(pcm), loop).result()

    processing_context = UserDialogProcessingContext()
    sst = SSTNode(on_new_phrase=dialog_context_builder.append_user_phrase)
    chat = ChatNode(context=dialup, on_new_phrase=dialog_context_builder.append_llm_phrase)
    tts = TTSNode(dialup)
    output = OutputNode(output_callback)
    ( processing_context
        .connect(sst)
        .connect(chat)
        .connect(tts)
        .connect(output) )

    try:
        while True:
            message = await ws.receive()
            if message.get("type") == "websocket.disconnect":
                client_disconnected.set()
                logger.info("Process-call disconnected by client for %s", context_id)
                break

            if "text" in message:
                text_data: str = message["text"]
                if text_data == "end_call":
                    logger.info(
                        "Closing process-call for %s (dialup %s): end_call received",
                        context_id,
                        dialup_id,
                    )
                    transcript = dialog_context_builder.get()
                    send_dialog_context(context_id, transcript)
                    complete_dialup(context_id, dialup_id)
                    completed = True
                    logger.info("Process-call completed and persisted for %s", context_id)
                    await ws.close(code=status.WS_1000_NORMAL_CLOSURE)
                    return
                processing_context.process(text_data)

            elif "bytes" in message:
                audio_data = np.frombuffer(message["bytes"], dtype=np.int16)
                processing_context.process(audio_data)
    finally:
        client_disconnected.set()
        logger.info("Stopping process-call pipeline for %s", context_id)
        await asyncio.to_thread(processing_context.close)
        if not completed:
            try:
                send_dialog_context(context_id, dialog_context_builder.get())
                mark_dialog_disconnected(context_id, dialup_id)
                logger.info("Disconnected process-call persisted for %s", context_id)
            except Exception:
                logger.exception("Could not persist disconnected call %s", context_id)
        logger.info("Process-call cleanup finished for %s", context_id)
