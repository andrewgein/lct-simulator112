import unittest
from unittest.mock import MagicMock, call

from app.application.service.dialog_service import DialogService
from app.domain.model import (
    CallDirection, CallScenario, CounterpartyType, DialogProgress, DialogStatus,
    DialogTranscript, Gender, Person,
)


class DialogServiceTests(unittest.TestCase):
    def setUp(self):
        self.port = MagicMock()
        self.service = DialogService(self.port)
        self.call = CallScenario("call-1", 1, CallDirection.INBOUND,
                                 CounterpartyType.CALLER, Person(), Gender.WOMEN,
                                 (), (), "", "WORRIED")

    def test_starts_next_call_using_new_call_identifier(self):
        self.port.get_progress.return_value = DialogProgress("context-1", "", DialogStatus.IDLE)
        self.port.get_next_call.return_value = self.call

        result = self.service.next_call("context-1")

        self.assertEqual(self.call, result)
        self.port.get_next_call.assert_called_once_with("context-1", "-1")
        self.port.start_call.assert_called_once_with("context-1", "call-1")

    def test_selects_requested_call_instead_of_first_available(self):
        self.port.get_progress.return_value = DialogProgress("context-1", "", DialogStatus.IDLE)
        self.port.get_call.return_value = self.call

        result = self.service.select_call("context-1", "call-1")

        self.assertEqual(self.call, result)
        self.port.get_call.assert_called_once_with("context-1", "call-1")
        self.port.start_call.assert_called_once_with("context-1", "call-1")
        self.port.get_next_call.assert_not_called()

    def test_cannot_select_another_call_during_active_conversation(self):
        self.port.get_progress.return_value = DialogProgress(
            "context-1", "call-2", DialogStatus.IN_CALL)

        with self.assertRaisesRegex(ValueError, "Другой звонок уже активен"):
            self.service.select_call("context-1", "call-1")

        self.port.start_call.assert_not_called()

    def test_resumes_disconnected_call(self):
        self.port.get_progress.return_value = DialogProgress(
            "context-1", "call-1", DialogStatus.DISCONNECTED)
        self.port.get_call.return_value = self.call

        result = self.service.resume_call("context-1")

        self.assertEqual(self.call, result)
        self.port.start_call.assert_called_once_with("context-1", "call-1")

    def test_persists_transcript_before_completing_call(self):
        transcript = DialogTranscript(())

        self.service.complete("context-1", "call-1", transcript)

        self.assertEqual(
            [call.append_transcript("context-1", transcript),
             call.complete_call("context-1", "call-1")],
            self.port.method_calls,
        )


if __name__ == "__main__":
    unittest.main()
