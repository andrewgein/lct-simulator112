import logging
import queue
import threading

logger = logging.getLogger(__name__)
_STOP = object()


class UserDialogProcessingNode:
    def __init__(self):
        self.input_queue = queue.Queue()
        self.output_queue = queue.Queue()
        self.stop_event = threading.Event()
        self._stop_lock = threading.Lock()
        self.worker_thread = threading.Thread(target=self.__process_new_event)

    def set_input_queue(self, input_queue: queue.Queue):
        self.input_queue = input_queue

    def set_output_queue(self, output_queue: queue.Queue):
        self.output_queue = output_queue

    def get_input_queue(self):
        return self.input_queue

    def get_output_queue(self):
        return self.output_queue

    def start(self):
        with self._stop_lock:
            if self.stop_event.is_set():
                raise RuntimeError("Cannot start a closed pipeline node")
            self.worker_thread.start()

    def stop(self, *, drain=False):
        """Join the active handler; optionally finish already queued events first.

        A sentinel wakes an idle worker immediately. Returning means callbacks
        have finished, which is required before taking the final transcript.
        """
        with self._stop_lock:
            if not self.stop_event.is_set():
                self.stop_event.set()
                if not drain or self.worker_thread.ident is None:
                    while True:
                        try:
                            self.input_queue.get_nowait()
                            self.input_queue.task_done()
                        except queue.Empty:
                            break
                if self.worker_thread.ident is not None:
                    self.input_queue.put(_STOP)
        if self.worker_thread.ident is not None and threading.current_thread() is not self.worker_thread:
            self.worker_thread.join(timeout=5.0)

    def _event_handler(self, event):
        pass

    def __process_new_event(self):
        while True:
            event = self.input_queue.get()
            try:
                if event is _STOP:
                    return
                self._event_handler(event)
            except Exception:
                logger.exception("Voice pipeline handler failed")
            finally:
                self.input_queue.task_done()
