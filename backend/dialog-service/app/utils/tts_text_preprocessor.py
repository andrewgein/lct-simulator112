from functools import lru_cache
import logging
from threading import Lock

from silero_stress import load_accentor


logger = logging.getLogger(__name__)

class TTSTextPreprocessor:

    def __init__(self):
        self._accentor = load_accentor()
        self._lock = Lock()

    @lru_cache(maxsize=512)
    def process(self, text: str) -> str:
        if not text.strip():
            return text

        try:
            with self._lock:
                accented_text = self._accentor(text)

        except Exception:
            logger.exception('Using original text')
            return text

        return accented_text
