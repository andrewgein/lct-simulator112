import queue

from app.grpc.com.simulator112.context.context_service_pb2 import DialogContext, Phrase, SpeakerType
from .processing_node import UserDialogProcessingNode


class UserDialogProcessingContext:
    def __init__(self):
        self.node_list = []
        self.transcript = []

    def get_input_queue(self) -> queue.Queue | None:
        if (len(self.node_list) == 0):
            return None
        return self.node_list[0].get_input_queue()

    def get_output_queue(self) -> queue.Queue | None:
        if (len(self.node_list) == 0):
            return None
        return self.node_list[-1].get_output_queue()

    def connect(self, node: UserDialogProcessingNode):
        if (len(self.node_list) != 0):
            node.set_input_queue( self.node_list[-1].get_output_queue() )

        self.node_list.append(node)
        node.start()
        return self

    def process(self, data):
        input_queue = self.get_input_queue()
        if input_queue is not None:
            input_queue.put(data)

    def close(self):
        for node in self.node_list:
            node.stop()
        output_queue = self.get_output_queue()
        if output_queue is not None:
            output_queue.join()
