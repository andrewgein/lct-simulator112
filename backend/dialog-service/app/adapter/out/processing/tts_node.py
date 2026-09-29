from .processing_node import UserDialogProcessingNode
from app.adapter.out.processing.tts_model import TTSError, TTSModel
from app.adapter.out.processing.voice_profiles import get_voice_gender, select_voice_profile
from app.domain.model import CallScenario, CounterpartyType
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
        self.speed = 1.25 if call.counterparty in (CounterpartyType.BRIGADE, CounterpartyType.SERVICE) else 1.15
        logger.info("Selected TTS voice profile: %s", self.voice_profile.id)

    def _event_handler(self, event):
        logger.info("Start audio generation")
        try:
            for chunk in self.model.generate(text=event, profile=self.voice_profile, speed=self.speed):
                audio = np.asarray(chunk.audio, dtype=np.float32)
                self.output_queue.put(audio)
                logger.info("New audio chunk")
        except TTSError as exc:
            logger.error("Speech synthesis unavailable: %s", exc)
            return

        logger.info("End of audio generation")
