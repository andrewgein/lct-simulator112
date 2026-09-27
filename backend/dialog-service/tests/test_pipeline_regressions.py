"""Regression coverage for number normalization and pipeline lifecycle."""
import asyncio
from queue import Queue
from types import SimpleNamespace
from unittest.mock import AsyncMock, Mock, patch

import numpy as np
import pytest

from app.adapter.out.processing.tts_text_preprocessor import normalize_numbers
from app.adapter.out.processing.chat_node import ChatNode
from app.adapter.out.processing.tts_model import AudioChunk
from app.adapter.out.processing.tts_node import TTSNode
from app.adapter.out.mock.context_adapter import _mock_call
from app.domain.model import DialogProgress, DialogStatus


@pytest.mark.parametrize('raw,expected', [
    ('нет 5 машин', 'нет пяти машин'),
    ('не хватает 2 машин', 'не хватает двух машин'),
    ('более 5 минут', 'более пяти минут'),
    ('в течение 5 минут', 'в течение пяти минут'),
    ('вижу 2 пострадавших', 'вижу двух пострадавших'),
    ('2 новые машины', 'две новые машины'),
])
def test_context(raw, expected):
    assert normalize_numbers(raw) == expected


@pytest.mark.parametrize('raw,expected', [
    ('38.5-39.5', 'от 38 целых 5 десятых до 39 целых 5 десятых'),
    ('12:30-14:00', 'от 12 часов 30 минут до 14 часов 00 минут'),
    ('5-10.5', 'от 5 до 10 целых 5 десятых'),
    ('8 (961) 263-36-64', '8 961 263 36 64'),
])
def test_structures(raw, expected):
    assert normalize_numbers(raw) == expected


class BareChat(ChatNode):
    def __del__(self):
        pass


def test_cancel_keeps_already_emitted_sentence_in_transcript():
    async def run():
        blocked = asyncio.Event()
        class Model:
            async def generate_answer(self, text):
                yield 'На 3 этаже.'
                blocked.set()
                await asyncio.Event().wait()
        node = object.__new__(BareChat)
        node.output_queue = Queue()
        node.model = Model()
        transcript = []
        node.on_new_phrase = transcript.append
        task = asyncio.create_task(node._llm_worker('Где вы?'))
        await blocked.wait()
        assert node.output_queue.get_nowait() == 'На 3 этаже.'
        task.cancel()
        assert await task == 'На 3 этаже.'
        assert transcript == ['На 3 этаже.']
    asyncio.run(run())


def test_stop_shuts_down_event_loop_thread():
    with patch('app.adapter.out.processing.chat_node.LLMModel', return_value=Mock(close=AsyncMock())):
        node = BareChat(_mock_call())
    node.start()
    try:
        node.stop()
        assert not node.loop_thread.is_alive()
    finally:
        if not node.loop.is_closed():
            node.loop.call_soon_threadsafe(node.loop.stop)
            node.loop_thread.join(timeout=2)
            node.loop.close()


class BareTTS(TTSNode):
    def __del__(self):
        pass


def test_pcm_sample_rate_is_respected():
    node = object.__new__(BareTTS)
    import threading
    node.stop_event = threading.Event()
    node.model = SimpleNamespace(generate=lambda **kw: iter([AudioChunk(np.zeros(16000), 16000)]))
    node.voice_profile = None
    node.output_queue = Queue()
    node._event_handler('Тест')
    # Frontend and recorder consume 24 kHz; 1 second input must remain 1 second.
    assert len(node.output_queue.get_nowait()) == 24000


def setup_controller(monkeypatch):
    from app.adapter.inbound.websocket import controller
    dialog = Mock()
    dialog.session.return_value.progress = DialogProgress('ctx', 'call', DialogStatus.IN_CALL)
    dialog.resume_call.return_value = _mock_call()
    recorder = Mock()
    recorder_factory = Mock(create=Mock(return_value=recorder))
    factory = Mock()
    monkeypatch.setattr(controller, '_dialog_use_case', dialog)
    monkeypatch.setattr(controller, '_voice_pipeline_factory', factory)
    monkeypatch.setattr(controller, '_call_recorder_factory', recorder_factory)
    ws = SimpleNamespace(query_params={'contextId': 'ctx'}, accept=AsyncMock(), close=AsyncMock())
    return controller, dialog, recorder, factory, ws


def test_pipeline_setup_failure_runs_cleanup(monkeypatch):
    controller, dialog, recorder, factory, ws = setup_controller(monkeypatch)
    factory.create.side_effect = RuntimeError('voice initialization failed')
    with pytest.raises(RuntimeError, match='voice initialization'):
        asyncio.run(controller.process_call(ws))
    recorder.close.assert_called_once()
    dialog.disconnect.assert_called_once()


def test_end_call_saves_final_callbacks(monkeypatch):
    controller, dialog, recorder, factory, ws = setup_controller(monkeypatch)
    pipeline = Mock()
    saved = []
    def create(**kwargs):
        # A queued STT callback completes when the worker is joined during shutdown.
        pipeline.close.side_effect = lambda: kwargs['on_operator_phrase']('Последняя фраза')
        return pipeline
    factory.create.side_effect = create
    dialog.complete.side_effect = lambda context, call, transcript: saved.append(transcript)
    ws.receive = AsyncMock(return_value={'type': 'websocket.receive', 'text': 'end_call'})
    asyncio.run(controller.process_call(ws))
    assert [p.text for p in saved[0].phrases] == ['Последняя фраза']


def test_session_rpc_does_not_block_other_async_tasks(monkeypatch):
    import threading
    from fastapi import WebSocketDisconnect
    from app.application.model.session import DialogSession
    controller, dialog, recorder, factory, ws = setup_controller(monkeypatch)
    heartbeat = threading.Event()
    def slow_session(context_id):
        # A synchronous call on the event loop would prevent this heartbeat
        # from running. Avoid fragile sub-millisecond timing assertions.
        assert heartbeat.wait(2), "RPC blocked the event loop"
        return DialogSession(DialogProgress('ctx', '', DialogStatus.IDLE), None)
    dialog.session.side_effect = slow_session
    ws.receive_json = AsyncMock(side_effect=WebSocketDisconnect())
    ws.send_json = AsyncMock()
    async def run():
        async def beat():
            await asyncio.sleep(0)
            heartbeat.set()
        ticker = asyncio.create_task(beat())
        await controller.dialog_session(ws)
        await ticker
    asyncio.run(run())
    ws.send_json.assert_awaited_once_with({'type': 'idle'})


def test_stop_cancels_active_generation_and_closes_client():
    import threading
    ready = threading.Event()
    finalized = threading.Event()
    phrases = []
    class Model:
        close = AsyncMock()
        async def generate_answer(self, text):
            try:
                yield 'Уже сказано. Незавершённый хвост 38.'
                ready.set()
                await asyncio.Event().wait()
            finally:
                finalized.set()
    model = Model()
    with patch('app.adapter.out.processing.chat_node.LLMModel', return_value=model):
        node = ChatNode(_mock_call(), on_new_phrase=phrases.append)
    node.start()
    try:
        node.input_queue.put('Вопрос')
        assert ready.wait(2)
        node.stop()
        node.stop()  # shutdown is idempotent
        assert finalized.is_set()
        assert phrases == ['Уже сказано.']
        assert not node.worker_thread.is_alive()
        assert not node.loop_thread.is_alive()
        assert node.loop.is_closed()
        model.close.assert_awaited_once()
    finally:
        node.stop()


def test_model_construction_failure_does_not_start_thread():
    with patch('app.adapter.out.processing.chat_node.LLMModel', side_effect=ValueError('bad config')), \
            patch('app.adapter.out.processing.chat_node.threading.Thread.start') as start:
        with pytest.raises(ValueError, match='bad config'):
            ChatNode(_mock_call())
        start.assert_not_called()


def test_base_node_can_close_before_start_and_drain_queue():
    from app.adapter.out.processing.processing_node import UserDialogProcessingNode
    unstarted = UserDialogProcessingNode()
    unstarted.stop()
    unstarted.stop()
    events = []
    class Node(UserDialogProcessingNode):
        def _event_handler(self, event):
            events.append(event)
    node = Node()
    for i in range(10):
        node.input_queue.put(i)
    node.start()
    node.stop(drain=True)
    assert events == list(range(10))
    assert not node.worker_thread.is_alive()
    assert node.input_queue.unfinished_tasks == 0


def test_disconnect_saves_callbacks_even_when_recording_fails(monkeypatch):
    controller, dialog, recorder, factory, ws = setup_controller(monkeypatch)
    recorder.close.side_effect = RuntimeError('S3 unavailable')
    def create(**kwargs):
        return Mock(close=lambda: kwargs['on_counterparty_phrase']('Последний ответ'))
    factory.create.side_effect = create
    ws.receive = AsyncMock(return_value={'type': 'websocket.disconnect'})
    asyncio.run(controller.process_call(ws))
    dialog.complete.assert_not_called()
    transcript = dialog.disconnect.call_args.args[2]
    assert [p.text for p in transcript.phrases] == ['Последний ответ']


@pytest.mark.parametrize('regenerate', [False, True])
def test_llm_http_stream_closed_when_consumer_stops(regenerate):
    from app.adapter.out.processing.llm_model import LLMModel
    class Stream:
        close = AsyncMock()
        async def __aiter__(self):
            yield SimpleNamespace(choices=[SimpleNamespace(delta=SimpleNamespace(content='Ответ.'))])
            await asyncio.Event().wait()
    stream = Stream()
    model = object.__new__(LLMModel)
    model.client = SimpleNamespace(chat=SimpleNamespace(completions=SimpleNamespace(create=AsyncMock(return_value=stream))))
    model.system_message = {'role': 'system', 'content': 'test'}
    model.model = 'test'
    model.dialog_history = []
    async def run():
        generator = model.regenerate_answer('Новый', 'Старый', 'Часть.') if regenerate else model.generate_answer('Вопрос')
        assert await anext(generator) == 'Ответ.'
        await generator.aclose()
        stream.close.assert_awaited_once()
    asyncio.run(run())


def test_stt_drains_audio_and_flushes_final_phrase_on_close():
    import importlib
    import sys
    recognizer = Mock()
    recognized = []
    def accept(data):
        recognized.append(data)
    recognizer.AcceptWaveform.side_effect = accept
    recognizer.FinalResult.side_effect = lambda: '{"text": "последняя фраза"}' if recognized else '{"text": ""}'
    vosk = SimpleNamespace(Model=Mock(), KaldiRecognizer=Mock(return_value=recognizer))
    with patch.dict(sys.modules, {'vosk': vosk}):
        module = importlib.import_module('app.adapter.out.processing.sst_node')
        phrases = []
        node = module.SSTNode(on_new_phrase=phrases.append)
        node.input_queue.put(np.array([1, 2], dtype=np.int16))
        node.start()
        node.stop()
        assert phrases == ['последняя фраза']
        assert not node.worker_thread.is_alive()
        assert node.input_queue.unfinished_tasks == 0


def test_partial_pipeline_construction_closes_started_nodes():
    import importlib
    import sys
    recognizer = Mock(FinalResult=Mock(return_value='{"text": ""}'))
    vosk = SimpleNamespace(Model=Mock(), KaldiRecognizer=Mock(return_value=recognizer))
    with patch.dict(sys.modules, {'vosk': vosk}):
        module = importlib.import_module('app.adapter.out.processing.voice_pipeline_adapter')
        created = []
        def chat(**kwargs):
            node = ChatNode(**kwargs)
            created.append(node)
            return node
        model = Mock(close=AsyncMock())
        with patch('app.adapter.out.processing.chat_node.LLMModel', return_value=model), \
                patch.object(module, 'ChatNode', side_effect=chat), \
                patch.object(module, 'TTSNode', side_effect=ValueError('bad voice')):
            with pytest.raises(ValueError, match='bad voice'):
                module.ProcessingVoicePipeline(_mock_call(), Mock(), Mock(), Mock())
        assert len(created) == 1
        assert not created[0].loop_thread.is_alive()
        assert not created[0].worker_thread.is_alive()
        model.close.assert_awaited_once()


def test_cancel_during_pipeline_construction_still_closes_it(monkeypatch):
    import threading
    controller, dialog, recorder, factory, ws = setup_controller(monkeypatch)
    entered = threading.Event()
    release = threading.Event()
    pipeline = Mock()
    def create(**kwargs):
        entered.set()
        assert release.wait(2)
        return pipeline
    factory.create.side_effect = create
    async def run():
        task = asyncio.create_task(controller.process_call(ws))
        assert await asyncio.to_thread(entered.wait, 2)
        task.cancel()
        release.set()
        with pytest.raises(asyncio.CancelledError):
            await task
    asyncio.run(run())
    pipeline.close.assert_called_once()
    recorder.close.assert_called_once()
    dialog.disconnect.assert_called_once()
