import logging
from vosk import Model, KaldiRecognizer
from .processing_node import UserDialogProcessingNode
import json

stt_model = Model('./resources/vosk-model-small-ru-0.22')
# TODO: sample rate may vary
sample_rate = 44100

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

    def get_final_text(self) -> str:
        text = json.loads(self.recognizer.FinalResult())
        return text["text"]

