from .processing_node import UserDialogProcessingNode
from app.utils.tts_model import TTSError, TTSModel
from app.utils.voice_profiles import get_voice_gender, select_voice_profile
from app.grpc.com.simulator112.incident.incident_context_pb2 import (
    DialupContext,
    EmotionalState,
)
import numpy as np
import logging

logger = logging.getLogger()


class TTSNode(UserDialogProcessingNode):
    def __init__(self, dialup: DialupContext):
        super().__init__()
        self.model = TTSModel()
        details = dialup.dialup_details
        applicant_age = dialup.applicant.age
        emotional_state = EmotionalState.Value(details.emotional_state or "WORRIED")
        voice_gender = get_voice_gender(applicant_age, details.gender)
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
                self.output_queue.put(audio)
                logger.info("New audio chunk")
        except TTSError as exc:
            logger.error("Speech synthesis unavailable: %s", exc)
            return

        logger.info("End of audio generation")
