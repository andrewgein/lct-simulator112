import logging
from vosk import Model, KaldiRecognizer
from .processing_node import UserDialogProcessingNode
import json

stt_model = Model('./resources/vosk-model-small-ru-0.22')
# Must match the AudioContext sample rate the frontend captures at
# (frontend/src/features/dialog/api/DialogApi.js).
sample_rate = 16000

logger = logging.getLogger(__name__)

class SSTNode(UserDialogProcessingNode):
    def __init__(self, on_new_phrase=lambda text: None):
        self.recognizer = KaldiRecognizer(stt_model, sample_rate)
        self.on_new_phrase = on_new_phrase
        super().__init__()

    def _event_handler(self, event):
        if isinstance(event, str):
            if event != "voice_stopped":
                raise ValueError("Received unknown command")
            else:
                final_text = self.get_final_text()
                if (final_text != ""):
                    logger.info("Text: " + final_text)
                    self.on_new_phrase(final_text)
                    self.output_queue.put(final_text)
                return

        self.recognizer.AcceptWaveform(event.tobytes())

    def stop(self, *, drain=True):
        # Preserve speech still queued when the socket closes, even when the
        # frontend did not send its final voice_stopped command.
        if not self.stop_event.is_set():
            self.input_queue.put("voice_stopped")
        super().stop(drain=drain)

    def get_final_text(self) -> str:
        text = json.loads(self.recognizer.FinalResult())
        return text["text"]
