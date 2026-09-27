"""Prepare completed Russian text for speech synthesis.

Numeric normalization happens before Silero's stress marking. Structural values
(phone numbers, times, decimals and ranges) are protected first; remaining
numbers are inflected from the morphology of the following word.
"""

from functools import lru_cache
import logging
import re
from threading import Lock

from num2words import num2words
from pymorphy3 import MorphAnalyzer
from silero_stress import load_accentor

logger = logging.getLogger(__name__)

_MORPH = MorphAnalyzer()
_CASE_MAP = {
    "nomn": "n", "gent": "g", "datv": "d", "accs": "a",
    "ablt": "i", "loct": "p", "loc2": "p",
}
_ORDINAL_WORDS = {
    "этаж", "подъезд", "квартира", "дом", "улица",
    "район", "класс", "серия",
}
_DIGITS = "ноль один два три четыре пять шесть семь восемь девять".split()


def _text_number(value: int, case: str = "n", gender: str = "m") -> str:
    """Convert with num2words' Russian case and gender support."""
    try:
        return num2words(value, lang="ru", case=case, gender=gender)
    except (TypeError, NotImplementedError, ValueError):
        # Keep the spoken value even if a particular num2words release cannot
        # inflect an uncommon form.
        return num2words(value, lang="ru")


def _following_word(text: str, end: int):
    match = re.match(r"\s+([А-Яа-яЁё-]+)", text[end:])
    if not match:
        return None
    return match.group(1), end + match.start(1), end + match.end(1)


def _normalize_ordinary_numbers(text: str) -> str:
    out = []
    cursor = 0
    for match in re.finditer(r"(?<![\w.])\d+(?![\w.])", text):
        out.append(text[cursor:match.start()])
        value = int(match.group())
        following = _following_word(text, match.end())
        parses = _MORPH.parse(following[0]) if following else []
        nominal = [
            p for p in parses
            if "NOUN" in p.tag or "ADJF" in p.tag or "PRTF" in p.tag
        ]
        noun = nominal[0] if nominal else None

        if value % 10 in {2, 3, 4} and value % 100 not in {12, 13, 14}:
            plural_nominative = next(
                (p for p in nominal if p.tag.case == "nomn" and p.tag.number == "plur"),
                None,
            )
            if plural_nominative:
                noun = plural_nominative
        case = _CASE_MAP.get(str(noun.tag.case), "n") if noun else "n"
        gender = {"femn": "f", "neut": "n", "masc": "m"}.get(
            str(noun.tag.gender), "m"
        ) if noun else "m"
        next_lemma = noun.normal_form if noun else ""
        prefix = text[:match.start()]
        age_phrase = bool(
            next_lemma == "год"
            and re.search(r"\b(?:мне|тебе|ему|ей|нам|вам|им|реб[её]нку)\s*$", prefix, re.I)
        )
        if age_phrase:
            # In "мне 35 лет" the numeral is nominative/accusative even
            # though the age noun itself is genitive plural.
            case = "n"
        ordinal_requested = next_lemma in _ORDINAL_WORDS or (
            next_lemma == "год" and bool(re.search(r"\bв\s*$", prefix, re.I))
        )
        if (
            ordinal_requested
            and not age_phrase
            and noun is not None
            and noun.tag.number == "sing"
            and value > 0
        ):
            spoken = num2words(
                value, lang="ru", to="ordinal", case=case, gender=gender,
            )
        else:
            spoken = _text_number(value, case, gender)
        out.append(spoken)
        cursor = match.end()
    out.append(text[cursor:])
    return "".join(out)


def normalize_numbers(text: str) -> str:
    """Normalize structured and inflected numbers in a completed sentence."""
    protected = []

    def hold(value: str) -> str:
        token = f"\uE000{chr(0xE100 + len(protected))}\uE001"
        protected.append(value)
        return token

    # Telephone patterns retain every digit and grouping, including leading 0.
    phone = re.compile(r"(?<!\w)(?:\+?7|8)(?:[\s()-]*\d){10}(?!\d)")

    def format_phone(match):
        digits = re.sub(r"\D", "", match.group())
        if len(digits) == 11:
            groups = f"{digits[0]} {digits[1:4]} {digits[4:7]} {digits[7:9]} {digits[9:11]}"
            return hold(("плюс " if match.group().lstrip().startswith("+") else "") + groups)
        return hold(" ".join(digits))

    text = phone.sub(format_phone, text)
    text = re.sub(
        r"(?<!\d)(\d{1,3})\.(\d+)(?!\d)",
        lambda m: hold(f"{m.group(1)} целых {m.group(2)} десятых"), text,
    )
    text = re.sub(
        r"(?<!\d)([01]?\d|2[0-3]):([0-5]\d)(?!\d)",
        lambda m: hold(f"{m.group(1)} часов {m.group(2)} минут"), text,
    )
    text = re.sub(
        r"(?<!\d)(\d+)\s*[-–—]\s*(\d+)(?!\d)",
        lambda m: hold(f"от {m.group(1)} до {m.group(2)}"), text,
    )
    text = re.sub(
        r"(?<![\w])-(\d+)(?!\w)",
        lambda m: "минус " + m.group(1), text,
    )
    text = _normalize_ordinary_numbers(text)
    return re.sub(
        r"\uE000([\uE100-\uEFFF])\uE001",
        lambda m: protected[ord(m.group(1)) - 0xE100], text,
    )


class TTSTextPreprocessor:
    def __init__(self):
        self._accentor = load_accentor()
        self._lock = Lock()

    @lru_cache(maxsize=512)
    def process(self, text: str) -> str:
        if not text.strip():
            return text
        normalized = normalize_numbers(text)
        try:
            with self._lock:
                return self._accentor(normalized)
        except Exception:
            logger.exception("Using normalized text without stress marks")
            return normalized
