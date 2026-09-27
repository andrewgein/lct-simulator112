import queue
import threading
import logging

from .processing_node import UserDialogProcessingNode


class UserDialogProcessingContext:
    def __init__(self):
        self.node_list = []
        self._closed = False
        self._close_lock = threading.Lock()

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
        with self._close_lock:
            input_queue = self.get_input_queue()
            if not self._closed and input_queue is not None:
                input_queue.put(data)

    def close(self):
        with self._close_lock:
            if self._closed:
                return
            self._closed = True
            # Stop upstream first so no producer survives its downstream consumer.
            for node in self.node_list:
                try:
                    node.stop()
                except Exception:
                    logging.getLogger(__name__).exception("Could not stop pipeline node")
