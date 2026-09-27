class BufferedAudioProcessor extends AudioWorkletProcessor {
    buffer = new Int16Array(2048);
    bufferIndex = 0;

    // `sampleRate` is a global provided by AudioWorkletGlobalScope; it always
    // matches the AudioContext's actual rate, so this can't drift out of sync.
    iterationPeriod = 128 / sampleRate;
    energyOffset = 1e-8;
    energyTresholdRatioPositive = 2;
    energyTresholdRatioNegative = 0.5;
    energyTresholdPositive = 1e-8 * 2;
    energyTresholdNegative = 1e-8 * 0.5;
    energyIntegration = 1;

    voiceTrend = 0;
    voiceTrendMax = 50;
    voiceTrendMin = -50;
    voiceTrendStart = 10;
    voiceTrendEnd = -25;
    vadState = false;

    process(inputChannels) {
        const input = inputChannels[0][0];

        let rms = 0;
        for (let index = 0; index < input.length; index++) {
            rms += input[index] * input[index];
        }
        rms = Math.sqrt(rms / input.length) * 32767;

        const signal = rms - this.energyOffset;
        if (signal > this.energyTresholdPositive) {
            this.voiceTrend = Math.min(this.voiceTrend + 1, this.voiceTrendMax);
        } else if (signal < -this.energyTresholdNegative) {
            this.voiceTrend = Math.max(this.voiceTrend - 1, this.voiceTrendMin);
        } else if (this.voiceTrend > 0) {
            this.voiceTrend--;
        } else if (this.voiceTrend < 0) {
            this.voiceTrend++;
        }

        const start = this.voiceTrend > this.voiceTrendStart;
        const end = this.voiceTrend < this.voiceTrendEnd;
        const integration = signal * this.iterationPeriod * this.energyIntegration;
        this.energyOffset += integration > 0 || !end ? integration : integration * 10;
        this.energyOffset = Math.max(this.energyOffset, 0);
        this.energyTresholdPositive = this.energyOffset * this.energyTresholdRatioPositive;
        this.energyTresholdNegative = this.energyOffset * this.energyTresholdRatioNegative;

        if (start && !this.vadState) {
            this.vadState = true;
        } else if (end && this.vadState) {
            this.vadState = false;
            this.addToBuffer(input);
            this.flushBuffer();
            this.port.postMessage("voice_stopped");
        }

        if (this.vadState) {
            this.addToBuffer(input);
        }
        return true;
    }

    addToBuffer(samples) {
        for (const sample of samples) {
            const clampedSample = sample > 0 ? Math.min(1, sample) * 32767 : Math.max(-1, sample) * 32768;
            this.buffer[this.bufferIndex++] = clampedSample;
            if (this.bufferIndex === 2048) {
                this.flushBuffer();
            }
        }
    }

    flushBuffer() {
        if (this.bufferIndex === 2048) {
            this.port.postMessage(new Int16Array(this.buffer));
        } else if (this.bufferIndex === 0) {
            return;
        } else {
            this.port.postMessage(new Int16Array(this.buffer).subarray(0, this.bufferIndex));
        }
        this.bufferIndex = 0;
    }
}

registerProcessor("buffered-audio-processor", BufferedAudioProcessor);
