from contextlib import asynccontextmanager
from fastapi import FastAPI
from starlette.types import ASGIApp, Receive, Scope, Send
from app.input_server import router as input_server_router
from app.utils.tts_model import TTSModel
from dotenv import load_dotenv
from prometheus_fastapi_instrumentator import Instrumentator
import logging

logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(levelname)s - %(message)s")
logger = logging.getLogger(__name__)
load_dotenv()

@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Initializing TTS model")
    TTSModel()
    yield

app = FastAPI(lifespan=lifespan)


class ConnectionLoggingMiddleware:
    """Logs the source (host:port) of every incoming HTTP/WS connection.

    In prod, dialog-service runs on a home PC reachable only through a
    Tailscale tunnel from the app VM - this is the log line that confirms
    calls are actually arriving over that tunnel.
    """

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
