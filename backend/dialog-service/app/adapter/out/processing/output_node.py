from collections.abc import Callable
from .processing_node import UserDialogProcessingNode

class OutputNode(UserDialogProcessingNode):
    def __init__(self, output_callback: Callable):
        super().__init__()
        self.output_callback = output_callback

    def _event_handler(self, event):
        self.output_callback(event)
