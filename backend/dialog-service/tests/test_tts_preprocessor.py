import sys
from types import SimpleNamespace
from unittest.mock import Mock

import pytest

from app.adapter.out.processing.tts_text_preprocessor import (
    TTSTextPreprocessor,
    normalize_numbers,
)


@pytest.mark.parametrize("input_text, expected_output", [
    ("на 3 этаже", "на третьем этаже"),
    ("с 3 этажа", "с третьего этажа"),
    ("около 5 минут", "около пяти минут"),
    ("2 машины", "две машины"),
    ("помогли 21 пострадавшему", "помогли двадцати одному пострадавшему"),
    ("позвоните 89001234567", "позвоните 8 900 123 45 67"),
    ("температура 38.5", "температура 38 целых 5 десятых"),
    ("в 14:30", "в 14 часов 30 минут"),
    ("уже написано два", "уже написано два"),
    ("минус 5 градусов", "минус пять градусов"),
])
def test_requested_examples(input_text, expected_output):
    assert normalize_numbers(input_text) == expected_output


@pytest.mark.parametrize("input_text, expected_output", [
    ("+79001234567", "+7 900 123 45 67"),
    ("8-900-123-45-67", "8 900 123 45 67"),
    ("8 900 003 00 07", "8 900 003 00 07"),
    ("5-10 минут", "от 5 до 10 минут"),
    ("5–10 минут", "от 5 до 10 минут"),
    ("-5--2", "от минус 5 до минус 2"),
    ("-5 градусов", "минус пять градусов"),
    ("−5 градусов", "минус пять градусов"),
    ("+5 градусов", "плюс пять градусов"),
    ("-38.5", "минус 38 целых 5 десятых"),
    ("38,05", "38 целых 05 сотых"),
    ("1.1", "1 целая 1 десятая"),
    ("в 01:01", "в 01 час 01 минута"),
    ("код 0012", "код 0 0 1 2"),
    ("к 2 машинам", "к двум машинам"),
    ("над 2 машинами", "над двумя машинами"),
    ("в 3 квартиру", "в третью квартиру"),
    ("на 21 этаже", "на двадцать первом этаже"),
    ("5 минут", "пять минут"),
    ("2 пострадавших", "два пострадавших"),
    ("2, машины", "два, машины"),
    ("A12 12Б 3-й", "A12 12Б 3-й"),
    ("два и 2 машины", "два и две машины"),
    ("на 3 этаже, 2 машины, в 14:30", "на третьем этаже, две машины, в 14 часов 30 минут"),
    ("", ""),
    ("  \n", "  \n"),
])
def test_numbers_and_boundaries(input_text, expected_output):
    assert normalize_numbers(input_text) == expected_output


def test_large_digit_sequence_is_not_lost():
    digits = "9" * 40
    assert normalize_numbers(digits).replace(" ", "") == digits


def test_spelled_numbers_remain_unchanged():
    text = "двадцати одному, две, на третьем этаже"
    assert normalize_numbers(normalize_numbers(text)) == text


def test_normalization_precedes_stress_and_is_cached(monkeypatch):
    accentor = Mock(side_effect=lambda text: text)
    monkeypatch.setitem(sys.modules, "silero_stress", SimpleNamespace(load_accentor=lambda: accentor))
    preprocessor = TTSTextPreprocessor()
    assert preprocessor.process("на 3 этаже") == "на третьем этаже"
    assert preprocessor.process("на 3 этаже") == "на третьем этаже"
    accentor.assert_called_once_with("На третьем этаже")


def test_stress_failure_preserves_normalization(monkeypatch):
    accentor = Mock(side_effect=RuntimeError("unavailable"))
    monkeypatch.setitem(sys.modules, "silero_stress", SimpleNamespace(load_accentor=lambda: accentor))
    assert TTSTextPreprocessor().process("2 машины") == "две машины"


@pytest.mark.parametrize('raw, expected', [
    ('2 этажа', 'два этажа'),
    ('3 дома', 'три дома'),
    ('около 5 домов', 'около пяти домов'),
    ('через 2 года', 'через два года'),
    ('через 1 год', 'через один год'),
    ('на 2 улицах', 'на двух улицах'),
    ('2 пожарные машины', 'две пожарные машины'),
    ('28.09.2026', '28.09.2026'),
    ('1-2-3', '1-2-3'),
    ('38.5–39.5', 'от 38 целых 5 десятых до 39 целых 5 десятых'),
])
def test_counting_and_compound_numbers(raw, expected):
    assert normalize_numbers(raw) == expected


@pytest.mark.parametrize('raw, expected', [
    ('+7 (900) 003-00-07', 'плюс семь девять ноль ноль ноль ноль три ноль ноль ноль семь'),
    ('38.05', 'тридцать восемь целых пять сотых'),
    ('01:01', 'один час одна минута'),
    ('5-10', 'от пяти до десяти'),
    ('-5--2', 'от минус пяти до минус двух'),
    ('1.1-2.1', 'от одной целой одной десятой до двух целых одной десятой'),
    ('21:21-22:22', 'от двадцати одного часа двадцати одной минуты до двадцати двух часов двадцати двух минут'),
    ('код 0012', 'код ноль ноль один два'),
])
def test_spoken_structures(raw, expected):
    assert normalize_numbers(raw, spoken=True) == expected
    assert normalize_numbers(expected, spoken=True) == expected


def test_phone_digits_reach_stress_as_words(monkeypatch):
    accentor = Mock(side_effect=lambda text: text)
    monkeypatch.setitem(sys.modules, 'silero_stress', SimpleNamespace(load_accentor=lambda: accentor))
    result = TTSTextPreprocessor().process('Телефон 8 (900) 003-00-07.')
    assert result == 'Телефон восемь девять ноль ноль ноль ноль три ноль ноль ноль семь.'
    accentor.assert_called_once_with(result)
