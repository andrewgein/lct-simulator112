from app.application.model.session import DialogSession
from app.application.port.outbound import ContextPort
from app.domain.model import CallScenario, DialogStatus, DialogTranscript


class DialogService:
    def __init__(self, context_port: ContextPort):
        self._context = context_port

    def session(self, context_id: str) -> DialogSession:
        progress = self._context.get_progress(context_id)
        call = None
        if progress.status in (DialogStatus.IN_CALL, DialogStatus.DISCONNECTED) and progress.active_call_id:
            call = self._context.get_call(context_id, progress.active_call_id)
        return DialogSession(progress, call)

    def next_call(self, context_id: str) -> CallScenario:
        progress = self._context.get_progress(context_id)
        if progress.status in (DialogStatus.IN_CALL, DialogStatus.DISCONNECTED):
            return self._context.get_call(context_id, progress.active_call_id)
        previous = progress.active_call_id if progress.status == DialogStatus.COMPLETED else "-1"
        call = self._context.get_next_call(context_id, previous)
        self._context.start_call(context_id, call.id)
        return call

    def select_call(self, context_id: str, call_id: str) -> CallScenario:
        progress = self._context.get_progress(context_id)
        if progress.status == DialogStatus.IN_CALL and progress.active_call_id != call_id:
            raise ValueError("Другой звонок уже активен")
        call = self._context.get_call(context_id, call_id)
        self._context.start_call(context_id, call.id)
        return call

    def resume_call(self, context_id: str) -> CallScenario:
        session = self.session(context_id)
        if session.call is None:
            raise ValueError("В контексте нет активного звонка")
        if session.progress.status == DialogStatus.DISCONNECTED:
            self._context.start_call(context_id, session.call.id)
        return session.call

    def restart_call(self, context_id: str) -> CallScenario:
        session = self.session(context_id)
        if session.call is None:
            raise ValueError("В контексте нет активного звонка")
        self._context.clear_call_transcript(context_id, session.call.id)
        self._context.start_call(context_id, session.call.id)
        return session.call

    def transcript_for_resume(self, context_id: str, call_id: str) -> DialogTranscript:
        return self._context.get_call_transcript(context_id, call_id)

    def complete(self, context_id: str, call_id: str, transcript: DialogTranscript) -> None:
        self._context.append_transcript(context_id, call_id, transcript)
        self._context.complete_call(context_id, call_id)

    def disconnect(self, context_id: str, call_id: str, transcript: DialogTranscript) -> None:
        self._context.append_transcript(context_id, call_id, transcript)
        self._context.disconnect_call(context_id, call_id)
