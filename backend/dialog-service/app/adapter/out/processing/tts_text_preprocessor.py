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


        first_alpha = next((c for c in text if c.isalpha()), "")
        needs_lowering = first_alpha.islower()
        accentor_input = text[:1].upper() + text[1:] if needs_lowering else text

        try:
            with self._lock:
                accented_text = self._accentor(accentor_input)

        except Exception:
            logger.exception('Using original text')
            return text

        if needs_lowering:
            for index, char in enumerate(accented_text):
                if char.isalpha():
                    accented_text = accented_text[:index] + char.lower() + accented_text[index + 1:]
                    break

        return accented_text
