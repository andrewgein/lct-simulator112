from collections import deque
from os import getenv
from threading import Lock

_WINDOW_SIZE = 20

_THRESHOLDS_MS = {
    "llm": (float(getenv("LLM_LATENCY_MEDIUM_MS", "800")), float(getenv("LLM_LATENCY_HIGH_MS", "2000"))),
    "tts": (float(getenv("TTS_LATENCY_MEDIUM_MS", "1500")), float(getenv("TTS_LATENCY_HIGH_MS", "4000"))),
}


def _level_for(service: str, avg_ms: float) -> str:
    medium, high = _THRESHOLDS_MS[service]
    if avg_ms > high:
        return "high"
    if avg_ms > medium:
        return "medium"
    return "low"


class LatencyTracker:
    """Tracks a rolling window of time-to-first-response latencies per service.

    A single dialog-service process serves all calls (no multi-worker uvicorn),
    so an in-memory window is a valid proxy for current service load.
    """

    def __init__(self):
        self._lock = Lock()
        self._samples = {service: deque(maxlen=_WINDOW_SIZE) for service in _THRESHOLDS_MS}

    def record(self, service: str, seconds: float) -> None:
        with self._lock:
            self._samples[service].append(seconds * 1000)

    def snapshot(self) -> dict:
        with self._lock:
            samples = {service: list(values) for service, values in self._samples.items()}
        result = {}
        for service, values in samples.items():
            avg_ms = round(sum(values) / len(values)) if values else 0
            result[service] = {"avgMs": avg_ms, "level": _level_for(service, avg_ms) if values else "low"}
        return result


tracker = LatencyTracker()
