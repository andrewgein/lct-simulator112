from app.grpc.com.simulator112.context.context_service_pb2 import DialogContext, Phrase, SpeakerType

class DialogContextBuilder:
    def __init__(self):
        self.transcript = []

    def append_user_phrase(self, text: str):
        self.transcript.append(Phrase(speaker=SpeakerType.USER, text=text))

    def append_llm_phrase(self, text: str):
        self.transcript.append(Phrase(speaker=SpeakerType.LLM, text=text))

    def get(self) -> DialogContext:
        return DialogContext(transcript=self.transcript)
