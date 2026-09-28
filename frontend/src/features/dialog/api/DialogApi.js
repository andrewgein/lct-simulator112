let websocket = null, audioContext = null, audioStream = null;
let controlSocket = null;

export let isDialogStarted = false;

function websocketUrl(apiEndpoint, path, contextId) {
    const endpoint = apiEndpoint || window.location.origin;
    const base = endpoint.replace(/^http:/i, "ws:").replace(/^https:/i, "wss:");
    return `${base}${path}?contextId=${encodeURIComponent(contextId)}`;
}

function emit(type, detail = {}) {
    window.dispatchEvent(new CustomEvent(`dialog:${type}`, { detail }));
}

export function initializeDialogSession(apiEndpoint, contextId) {
    if (controlSocket?.readyState < WebSocket.CLOSING) return;

    controlSocket = new WebSocket(websocketUrl(apiEndpoint, "/api/v1/dialog/session", contextId));
    controlSocket.onerror = () => {
        emit("error", { message: "Не удалось подключиться к сервису диалога" });
    };
    controlSocket.onclose = () => {
        controlSocket = null;
        emit("error", { message: "Соединение с сервисом диалога закрыто" });
    };
    controlSocket.onmessage = (event) => {
        const message = JSON.parse(event.data);
        emit(message.type, message);
    };
}

export function requestNextCall() {
    if (controlSocket?.readyState === WebSocket.OPEN) {
        controlSocket.send(JSON.stringify({ type: "request_next_call" }));
    }
}

export function dismissCall() {
    if (controlSocket?.readyState === WebSocket.OPEN) {
        controlSocket.send(JSON.stringify({ type: "dismiss_call" }));
    }
}

export function requestCall(callId) {
    if (controlSocket?.readyState !== WebSocket.OPEN) return false;
    controlSocket.send(JSON.stringify({ type: "request_call", callId }));
    return true;
}

export function requestDialogStatus() {
    if (controlSocket?.readyState === WebSocket.OPEN) {
        controlSocket.send(JSON.stringify({ type: "request_status" }));
    }
}

export function startDialog(apiEndpoint, contextId) {
    const websocketEndpoint = websocketUrl(apiEndpoint, "/api/v1/dialog/process-call", contextId);
    audioContext = new AudioContext();
    audioContext.resume().catch((error) => console.error("Can't start audio playback", error));

    websocket = new WebSocket(websocketEndpoint);
    websocket.binaryType = "arraybuffer";
    websocket.onerror = (error) => {
        console.error("Can't open input-stream socket", error);
    };
    websocket.onopen = () => emit("call_started");
    websocket.onclose = cleanupCall;
    isDialogStarted = true;
    let nextTimestamp = 0;
    websocket.onmessage = (message) => {
        const pcmAudioBuffer = new Int16Array(message.data);
        const float32AudioBuffer = new Float32Array(pcmAudioBuffer.length);
        const int16ToFloat32Scale = 1 / 32768;
        for (let index = 0; index < pcmAudioBuffer.length; index++) {
            float32AudioBuffer[index] = pcmAudioBuffer[index] * int16ToFloat32Scale;
        }
        const audioBuffer = audioContext.createBuffer(1, float32AudioBuffer.length, 24000);
        audioBuffer.copyToChannel(float32AudioBuffer, 0);

        const source = audioContext.createBufferSource();
        source.buffer = audioBuffer;
        source.connect(audioContext.destination);
        if (nextTimestamp < audioContext.currentTime) {
            nextTimestamp = audioContext.currentTime + 0.05;
        }
        source.start(nextTimestamp);
        nextTimestamp += audioBuffer.duration;
    };

    navigator.mediaDevices
        .getUserMedia({ audio: true })
        .then((stream) => {
            audioStream = stream;
            const source = audioContext.createMediaStreamSource(audioStream);
            audioContext.audioWorklet.addModule("/audio/buffered-audio-processor.js")
                .then(() => {
                    const bufferedAudioProcessor = new AudioWorkletNode(audioContext, "buffered-audio-processor");
                    bufferedAudioProcessor.port.onmessage = (event) => {
                        if (websocket.readyState === WebSocket.OPEN) {
                            websocket.send(event.data);
                        }
                    };
                    source.connect(bufferedAudioProcessor);
                })
                .catch((error) => console.error("Can't load audio worklet module", error));
        })
        .catch((error) => console.error("Can't access user audio device", error));
}

function cleanupCall() {
    requestDialogStatus();
    audioContext?.close().catch(() => {});
    audioStream?.getTracks().forEach((track) => track.stop());
    websocket = null;
    audioContext = null;
    audioStream = null;
    isDialogStarted = false;
}

export function stopDialog() {
    if (websocket?.readyState === WebSocket.OPEN) {
        websocket.send("end_call");
    } else {
        cleanupCall();
    }
}
