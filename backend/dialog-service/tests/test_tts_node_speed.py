from types import SimpleNamespace
from unittest.mock import MagicMock, patch

from app.adapter.out.processing.tts_node import TTSNode
from app.domain.model import CallDirection, CallScenario, CounterpartyType, Gender, Person


def test_all_calls_use_normal_tts_speed():
    for counterparty in (
        CounterpartyType.CALLER,
        CounterpartyType.BRIGADE,
        CounterpartyType.SERVICE,
    ):
        call = CallScenario(
            id="call", position=0, direction=CallDirection.INBOUND,
            counterparty=counterparty, person=Person(), gender=Gender.MAN,
            known_facts=(), hidden_facts=(), ai_context="", emotional_state="CALM",
        )
        model = MagicMock()
        model.generate.return_value = iter(())
        with patch("app.adapter.out.processing.tts_node.TTSModel", return_value=model), \
             patch("app.adapter.out.processing.tts_node.select_voice_profile", return_value=SimpleNamespace(id="test")):
            node = TTSNode(call)
            node.worker_thread.start()
            try:
                node._event_handler("Проверка")
                model.generate.assert_called_once_with(text="Проверка", profile=node.voice_profile, speed=1.0)
            finally:
                node.stop()
