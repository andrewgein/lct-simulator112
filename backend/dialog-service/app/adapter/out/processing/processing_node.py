import queue
import threading
import traceback


class UserDialogProcessingNode:
    def __init__(self):
        self.input_queue = queue.Queue()
        self.output_queue = queue.Queue()
        self.stop_event = threading.Event()
        self.worker_thread = threading.Thread(target=self.__process_new_event, args=(self.stop_event,))

    def __del__(self):
        self.stop()


    def set_input_queue(self, input_queue: queue.Queue):
        self.input_queue = input_queue

    def set_output_queue(self, output_queue: queue.Queue):
        self.output_queue = output_queue

    def get_input_queue(self):
        return self.input_queue

    def get_output_queue(self):
        return self.output_queue


    def start(self):
        self.worker_thread.start()

    def stop(self):
        self.stop_event.set()

        while not self.input_queue.empty():
            try:
                self.input_queue.get_nowait()
                self.input_queue.task_done()
            except queue.Empty:
                break

        self.worker_thread.join(timeout=5.0)

    def _event_handler(self, event):
        pass

    def __process_new_event(self, stop_event: threading.Event):
        while not stop_event.is_set():
            try:
                event = self.input_queue.get(timeout=3)
            except queue.Empty:
                continue

            try:
                self._event_handler(event)
            except Exception as ex:
                print(f"_event_handler exception {ex}")
                traceback.print_exception(ex)
            finally:
                self.input_queue.task_done()
