import asyncio
from contextlib import asynccontextmanager
from fastapi import FastAPI
from starlette.types import ASGIApp, Receive, Scope, Send
from app.adapter.config.configuration import components
from app.adapter.inbound.websocket.controller import configure, router as input_server_router
from dotenv import load_dotenv
from prometheus_fastapi_instrumentator import Instrumentator
from os import getenv
import logging

logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(levelname)s - %(message)s")
logger = logging.getLogger(__name__)
load_dotenv()
application_components = components()
configure(
    application_components.dialog,
    application_components.voice_pipeline,
    application_components.call_recorder,
)


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Initializing TTS model")
    await asyncio.to_thread(application_components.voice_pipeline.warm_up)
    logger.info("TTS model ready")
    yield


app = FastAPI(lifespan=lifespan)


class ConnectionLoggingMiddleware:
    """Logs the source (host:port) of every incoming HTTP/WS connection"""

    def __init__(self, app: ASGIApp):
        self.app = app

    async def __call__(self, scope: Scope, receive: Receive, send: Send):
        if scope["type"] in ("http", "websocket"):
            client = scope.get("client")
            client_host = f"{client[0]}:{client[1]}" if client else "unknown"
            logger.info("%s %s from %s", scope["type"].upper(), scope.get("path"), client_host)
        await self.app(scope, receive, send)


app.add_middleware(ConnectionLoggingMiddleware)

Instrumentator().instrument(app).expose(app, endpoint="/metrics")

app.include_router(input_server_router)


@app.get("/health")
async def health():
    return {"status": "ok"}


@app.get("/internal/settings")
async def internal_settings():
    """Read-only view of the voice pipeline's current config."""
    return {
        "llmModel": getenv("LLM_MODEL", "gpt-oss-120b"),
        "llmBaseUrl": getenv("LLM_BASE_URL", "https://api.aitunnel.ru/v1/"),
        "ttsBaseUrl": getenv("F5_TTS_BASE_URL", ""),
        "ttsReadTimeout": getenv("F5_TTS_READ_TIMEOUT", "120"),
        "ttsVoiceProfile": getenv("TTS_VOICE_PROFILE"),
    }
