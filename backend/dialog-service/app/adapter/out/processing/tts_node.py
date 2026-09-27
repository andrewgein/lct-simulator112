from .processing_node import UserDialogProcessingNode
from app.adapter.out.processing.tts_model import OUTPUT_SAMPLE_RATE, TTSError, TTSModel
from app.adapter.out.processing.voice_profiles import get_voice_gender, select_voice_profile
from app.domain.model import CallScenario
import numpy as np
import logging

logger = logging.getLogger()


class TTSNode(UserDialogProcessingNode):
    def __init__(self, call: CallScenario):
        super().__init__()
        self.model = TTSModel()
        emotional_state = (call.emotional_state or "WORRIED").upper()
        voice_gender = get_voice_gender(call.person.age, call.gender)
        self.voice_profile = select_voice_profile(
            emotional_state,
            gender=voice_gender,
        )
        logger.info("Selected TTS voice profile: %s", self.voice_profile.id)

    def _event_handler(self, event):
        logger.info("Start audio generation")
        try:
            for chunk in self.model.generate(text=event, profile=self.voice_profile):
                audio = np.asarray(chunk.audio, dtype=np.float32)
                if self.stop_event.is_set():
                    break
                if chunk.sample_rate <= 0:
                    raise TTSError("Invalid TTS sample rate")
                if chunk.sample_rate != OUTPUT_SAMPLE_RATE and audio.size:
                    size = max(1, round(audio.size * OUTPUT_SAMPLE_RATE / chunk.sample_rate))
                    positions = np.arange(size) * chunk.sample_rate / OUTPUT_SAMPLE_RATE
                    audio = np.interp(positions, np.arange(audio.size), audio).astype(np.float32)
                self.output_queue.put(audio)
                logger.info("New audio chunk")
        except TTSError as exc:
            logger.error("Speech synthesis unavailable: %s", exc)
            return

        logger.info("End of audio generation")
