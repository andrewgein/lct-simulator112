"""Russian number normalization for complete TTS sentences.

Regex protects complete structural spans before morphology sees ordinary integers.
The readable form is useful in tests/tools; the spoken form expands every protected
number too, including individual telephone digits, before applying Silero stress.
"""
from functools import lru_cache
import logging
import re
from threading import Lock

from num2words import num2words
from pymorphy3 import MorphAnalyzer

logger = logging.getLogger(__name__)
_SPACE = r"[ \t\u00a0]*"
_SEP = r"[ \t\u00a0–—-]*"
_TIME = r"(?:[01]?\d|2[0-3]):[0-5]\d"
_NUMBER = r"[+−-]?\d+(?:[.,]\d+)?"
_ATOM = rf"(?:{_TIME}|{_NUMBER})"
_PHONE = rf"(?:\+7|8){_SEP}(?:\(\d{{3}}\)|\d{{3}}){_SEP}\d{{3}}{_SEP}\d{{2}}{_SEP}\d{{2}}"
_RANGE = rf"({_ATOM}){_SPACE}[-–—]{_SPACE}({_ATOM})"
_STRUCTURAL = re.compile(
    rf"(?<![\w+−.:/–—-])(?:"
    rf"(?P<phone>{_PHONE})|(?P<range>{_RANGE})|"
    # Unknown compound numbers (dates, versions, etc.) remain atomic.
    r"(?P<chain>[+−-]?\d+(?:[.:/–—-]\d+){2,})|"
    rf"(?P<time>{_TIME})|(?P<decimal>[+−-]?\d+[.,]\d+)"
    r")(?!\w|[.,:/–—-]\d)"
)
_INTEGER = re.compile(r"(?<!\w)[+−-]?\d+(?!\w|[-–—][^\W\d_])")
_NEXT_WORD = re.compile(r"\s+([^\W\d_]+)\b")
_PREVIOUS_WORD = re.compile(r"([^\W\d_]+)\s+$")
_CASES = {
    'nomn': 'nominative', 'gent': 'genitive', 'datv': 'dative',
    'accs': 'accusative', 'ablt': 'instrumental', 'loct': 'prepositional',
    'gen2': 'genitive', 'loc2': 'prepositional',
}
_GENDERS = {'masc': 'masculine', 'femn': 'feminine', 'neut': 'neuter'}
_ORDINAL_NOUNS = {'этаж', 'подъезд', 'квартира', 'дом', 'улица', 'год', 'район', 'класс', 'серия'}
_PREPOSITION_CASES = {
    'около': ('gent',), 'от': ('gent',), 'до': ('gent',), 'без': ('gent',),
    'из': ('gent',), 'у': ('gent',), 'для': ('gent',), 'после': ('gent',),
    'к': ('datv',), 'ко': ('datv',), 'над': ('ablt',), 'перед': ('ablt',),
    'между': ('ablt',), 'при': ('loct',), 'о': ('loct',), 'об': ('loct',),
    'в': ('loct', 'accs'), 'на': ('loct', 'accs'), 'с': ('gent', 'ablt'),
    'со': ('gent', 'ablt'), 'под': ('ablt', 'accs'), 'за': ('ablt', 'accs'),
    'через': ('accs',), 'про': ('accs',), 'по': ('datv', 'loct'),
    'более': ('gent',), 'менее': ('gent',), 'свыше': ('gent',),
    'нет': ('gent',), 'меньше': ('gent',), 'больше': ('gent',),
}
_GENITIVE_CONTEXT = re.compile(
    r"(?:\bв\s+(?:течение|продолжение)|\bне\s+хватает|\bне\s+было|\bне\s+будет)\s+$", re.I)
_ACCUSATIVE_VERBS = {'видеть', 'увидеть', 'встретить', 'встречать', 'спасти', 'спасать',
                     'найти', 'обнаружить', 'эвакуировать', 'вызвать', 'вызывать', 'слышать'}
_GENITIVE_VERBS = {'хватать', 'недоставать'}


@lru_cache(maxsize=1)
def _morph():
    return MorphAnalyzer()


@lru_cache(maxsize=2048)
def _parses(word):
    return tuple(_morph().parse(word))


def _form(digits, one, few, many):
    ending = int(digits[-2:])
    if 11 <= ending <= 14:
        return many
    return one if ending % 10 == 1 else few if 2 <= ending % 10 <= 4 else many


def _signed(value):
    if value.startswith(('-', '−')):
        return 'минус ' + value[1:]
    if value.startswith('+'):
        return 'плюс ' + value[1:]
    return value


def _digits(value):
    return ' '.join(num2words(int(d), lang='ru') for d in value)


def _words(value, *, case='nomn', gender='masc', ordinal=False, animate=False):
    if len(value) > 33:
        return _digits(value)
    return num2words(int(value), lang='ru', to='ordinal' if ordinal else 'cardinal',
                     case=_CASES.get(case, 'nominative'),
                     gender=_GENDERS.get(gender, 'masculine'), animate=animate)


def _atom(value, spoken, case='nomn'):
    if re.fullmatch(_TIME, value):
        hours, minutes = value.split(':')
        h = _words(hours, case=case) if spoken else hours
        m = _words(minutes, case=case, gender='femn') if spoken else minutes
        h_unit = _form(hours, 'часа', 'часов', 'часов') if case == 'gent' else _form(hours, 'час', 'часа', 'часов')
        m_unit = _form(minutes, 'минуты', 'минут', 'минут') if case == 'gent' else _form(minutes, 'минута', 'минуты', 'минут')
        return f'{h} {h_unit} {m} {m_unit}'
    if '.' in value or ',' in value:
        whole, fraction = re.split(r'[.,]', value)
        denominations = {
            1: ('десятая', 'десятых'), 2: ('сотая', 'сотых'),
            3: ('тысячная', 'тысячных'), 4: ('десятитысячная', 'десятитысячных'),
            5: ('стотысячная', 'стотысячных'), 6: ('миллионная', 'миллионных'),
        }
        if len(fraction) not in denominations:
            tail = _digits(fraction) if spoken else ' '.join(fraction)
            return f'{_atom(whole, spoken, case)} точка {tail}'
        one, many = denominations[len(fraction)]
        whole_unit = _form(whole.lstrip('+−-'), 'целая', 'целых', 'целых')
        fraction_unit = _form(fraction, one, many, many)
        if case == 'gent':
            whole_unit = 'целой' if whole_unit == 'целая' else 'целых'
            fraction_unit = one[:-2] + 'ой' if fraction_unit == one else many
        if spoken:
            head = _signed(whole[:len(whole) - len(whole.lstrip('+−-'))] + _words(whole.lstrip('+−-'), case=case, gender='femn'))
            tail = _words(fraction, case=case, gender='femn')
        else:
            head, tail = _signed(whole), fraction
        return f'{head} {whole_unit} {tail} {fraction_unit}'
    digits = value.lstrip('+−-')
    if not spoken:
        return _signed(value)
    words = _digits(digits) if len(digits) > 33 else _words(digits, case=case)
    return _signed(value[:len(value) - len(digits)] + words)


def _structure(match, spoken):
    value = match.group()
    if match.lastgroup == 'phone':
        digits = re.sub(r'\D', '', value)
        if spoken:
            return ('плюс ' if value.startswith('+') else '') + _digits(digits)
        return ('+' if value.startswith('+') else '') + f'{digits[0]} {digits[1:4]} {digits[4:7]} {digits[7:9]} {digits[9:]}'
    if match.lastgroup == 'range':
        bounds = re.fullmatch(_RANGE, value)
        case = 'gent' if spoken else 'nomn'
        return f'от {_atom(bounds[1], spoken, case)} до {_atom(bounds[2], spoken, case)}'
    if match.lastgroup == 'chain':
        if not spoken:
            return value
        # Spell unknown sequences instead of interpreting their punctuation as
        # arithmetic or throwing away leading zeros (e.g. dates and versions).
        separators = {'.': ' точка ', ':': ' двоеточие ', '/': ' дробь ', '-': ' тире ', '–': ' тире ', '—': ' тире '}
        return ''.join(_digits(part) if part.isdigit() else separators.get(part, part)
                       for part in re.split(r'(\d+)', value))
    return _atom(value, spoken)


def _following(text, start):
    """Keep adjective agreement, but obtain gender/animacy from its noun."""
    first = ()
    for _ in range(4):
        match = _NEXT_WORD.match(text, start)
        if not match:
            break
        candidates = tuple(p for p in _parses(match[1].lower()) if p.tag.POS in {'NOUN', 'ADJF', 'PRTF'})
        if not candidates:
            break
        if not first:
            first = candidates
        nouns = tuple(p for p in candidates if p.tag.POS == 'NOUN')
        adjectives = any(p.tag.POS in {'ADJF', 'PRTF'} for p in candidates)
        if nouns and not adjectives:
            return first, nouns
        start = match.end()
    return first, first


def _integer(match, text, spoken):
    value = match.group()
    digits = value.lstrip('+−-')
    sign = value[:len(value) - len(digits)]
    if (len(digits) > 1 and digits.startswith('0')) or len(digits) > 33:
        return _signed(sign + (_digits(digits) if spoken else ' '.join(digits)))
    number = int(digits)
    agreement, nouns = _following(text, match.end())
    prefix = text[:match.start()]
    preceding = _PREVIOUS_WORD.search(prefix)
    previous = preceding[1].lower() if preceding else ''
    allowed = _PREPOSITION_CASES.get(previous, ())
    if _GENITIVE_CONTEXT.search(prefix):
        allowed = ('gent',)
    elif previous:
        verbs = {p.normal_form for p in _parses(previous) if p.tag.POS in {'VERB', 'INFN'}}
        if verbs & _ACCUSATIVE_VERBS:
            allowed = ('accs',)
        elif verbs & _GENITIVE_VERBS:
            allowed = ('gent',)
    preferred = [p for p in nouns if p.tag.case in allowed]
    noun = next(iter(preferred or nouns), None)
    if noun is None:
        return _signed(sign + _words(digits, case=allowed[0] if len(allowed) == 1 else 'nomn'))
    case = noun.tag.case
    if len(allowed) == 1:
        case = allowed[0]
    elif agreement is not nouns and agreement:
        # Plural nominative adjectives disambiguate e.g. «две новые машины».
        agreed = next((p for p in agreement if p.tag.case in allowed), agreement[0])
        if agreed.tag.case in {'nomn', 'accs'} and not allowed:
            case = agreed.tag.case
    last = number % 10 if number % 100 not in range(11, 15) else 0
    counted_form = noun.tag.case == 'gent' and (
        (last in {2, 3, 4} and (noun.tag.number == 'sing' or noun.tag.POS != 'NOUN')) or
        (last not in {1, 2, 3, 4} and noun.tag.number == 'plur'))
    ordinal = (noun.normal_form in _ORDINAL_NOUNS and noun.tag.number == 'sing'
               and not (counted_form and (not allowed or allowed == ('accs',)))
               and not (noun.normal_form == 'год' and previous in {'через', 'за', 'на'})
               and not sign)
    if not ordinal and case == 'gent' and not allowed and counted_form:
        case = 'nomn'
    return _signed(sign + _words(digits, case=case, gender=noun.tag.gender,
                                ordinal=ordinal, animate=noun.tag.animacy == 'anim'))


@lru_cache(maxsize=512)
def normalize_numbers(text: str, *, spoken: bool = False) -> str:
    """Normalize digits only. Structural spans never enter contextual morphology."""
    result = []
    start = 0
    for match in _STRUCTURAL.finditer(text):
        plain = text[start:match.start()]
        result.append(_INTEGER.sub(lambda m: _integer(m, plain, spoken), plain))
        result.append(_structure(match, spoken))
        start = match.end()
    plain = text[start:]
    result.append(_INTEGER.sub(lambda m: _integer(m, plain, spoken), plain))
    return ''.join(result)


class TTSTextPreprocessor:
    def __init__(self):
        from silero_stress import load_accentor
        self._accentor = load_accentor()
        self._lock = Lock()

    @lru_cache(maxsize=512)
    def process(self, text: str) -> str:
        if not text.strip():
            return text
        text = normalize_numbers(text, spoken=True)
        first = next((i for i, char in enumerate(text) if char.isalpha()), None)
        needs_lowering = first is not None and text[first].islower()
        accentor_input = text[:first] + text[first].upper() + text[first + 1:] if needs_lowering else text
        try:
            with self._lock:
                accented_text = self._accentor(accentor_input)
        except Exception:
            logger.exception('Using normalized text without stress')
            return text
        if needs_lowering:
            for index, char in enumerate(accented_text):
                if char.isalpha():
                    accented_text = accented_text[:index] + char.lower() + accented_text[index + 1:]
                    break
        return accented_text
