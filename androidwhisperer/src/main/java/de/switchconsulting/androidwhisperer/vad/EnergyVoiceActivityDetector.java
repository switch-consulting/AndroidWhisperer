/*
 * Copyright 2026 Switch Consulting (https://switch-consulting.de/)
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://opensource.org/licenses/MIT
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.switchconsulting.androidwhisperer.vad;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Energy-based Voice Activity Detector (VAD) using Root Mean Square (RMS) decibel calculation.
 * <p>
 * Evaluates audio energy against a configurable decibel threshold (dB FS) over sliding frames.
 * Uses a hangover timer to prevent speech segmentation during natural pauses within utterances.
 * </p>
 *
 * @author Dirk Schnelle-Walka
 */
public class EnergyVoiceActivityDetector implements VoiceActivityDetector {

    public static final int DEFAULT_SAMPLE_RATE = 16_000;
    public static final int DEFAULT_FRAME_SIZE_MS = 20;
    public static final float DEFAULT_ENERGY_THRESHOLD_DB = -35.0f;
    public static final long DEFAULT_SILENCE_HANGOVER_MS = 1500L;
    public static final long DEFAULT_MIN_SPEECH_DURATION_MS = 100L;
    public static final float MIN_DB_FS = -100.0f;

    private final int sampleRate;
    private final int frameSizeMs;
    private final int frameSizeSamples;
    private final float energyThresholdDb;
    private final long silenceHangoverMs;
    private final long minSpeechDurationMs;

    private VoiceActivityListener listener;
    private VadState currentState = VadState.SILENCE;

    private final float[] sampleBuffer;
    private int bufferedCount = 0;

    private long consecutiveSpeechMs = 0L;
    private long consecutiveSilenceMs = 0L;

    /**
     * Constructs an EnergyVoiceActivityDetector with default parameters.
     */
    public EnergyVoiceActivityDetector() {
        this(new Builder());
    }

    private EnergyVoiceActivityDetector(Builder builder) {
        this.sampleRate = builder.sampleRate;
        this.frameSizeMs = builder.frameSizeMs;
        this.frameSizeSamples = (int) Math.round((double) sampleRate * frameSizeMs / 1000.0);
        this.energyThresholdDb = builder.energyThresholdDb;
        this.silenceHangoverMs = builder.silenceHangoverMs;
        this.minSpeechDurationMs = builder.minSpeechDurationMs;
        this.sampleBuffer = new float[this.frameSizeSamples];
    }

    @Override
    public synchronized void process(@NonNull float[] samples, int offset, int length) {
        if (samples == null) {
            throw new IllegalArgumentException("samples array cannot be null");
        }
        if (offset < 0 || length < 0 || offset + length > samples.length) {
            throw new IllegalArgumentException("Invalid offset or length bounds");
        }

        int index = offset;
        int remaining = length;

        while (remaining > 0) {
            int needed = frameSizeSamples - bufferedCount;
            int toCopy = Math.min(needed, remaining);

            System.arraycopy(samples, index, sampleBuffer, bufferedCount, toCopy);
            bufferedCount += toCopy;
            index += toCopy;
            remaining -= toCopy;

            if (bufferedCount == frameSizeSamples) {
                processFrame(sampleBuffer, 0, frameSizeSamples);
                bufferedCount = 0;
            }
        }
    }

    @Override
    public synchronized void processPcm16(@NonNull byte[] pcm16, int offset, int length) {
        if (pcm16 == null) {
            throw new IllegalArgumentException("pcm16 array cannot be null");
        }
        if (offset < 0 || length < 0 || offset + length > pcm16.length) {
            throw new IllegalArgumentException("Invalid offset or length bounds");
        }
        if (length % 2 != 0) {
            throw new IllegalArgumentException("PCM16 byte buffer length must be even");
        }

        int numSamples = length / 2;
        float[] floatSamples = new float[numSamples];
        ByteBuffer bb = ByteBuffer.wrap(pcm16, offset, length).order(ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < numSamples; i++) {
            floatSamples[i] = bb.getShort() / 32768.0f;
        }

        process(floatSamples, 0, numSamples);
    }

    private void processFrame(float[] frame, int offset, int length) {
        float rmsDb = calculateRmsDb(frame, offset, length);

        if (listener != null) {
            listener.onRmsChanged(rmsDb);
        }

        boolean active = rmsDb >= energyThresholdDb;

        if (active) {
            consecutiveSilenceMs = 0L;
            if (currentState == VadState.SILENCE) {
                consecutiveSpeechMs += frameSizeMs;
                if (consecutiveSpeechMs >= minSpeechDurationMs) {
                    currentState = VadState.SPEECH;
                    if (listener != null) {
                        listener.onSpeechStarted();
                    }
                }
            }
        } else {
            consecutiveSpeechMs = 0L;
            if (currentState == VadState.SPEECH) {
                consecutiveSilenceMs += frameSizeMs;
                if (consecutiveSilenceMs >= silenceHangoverMs) {
                    currentState = VadState.SILENCE;
                    consecutiveSilenceMs = 0L;
                    if (listener != null) {
                        listener.onSpeechEnded();
                    }
                }
            }
        }
    }

    /**
     * Calculates Root Mean Square (RMS) energy in decibels relative to full scale (dB FS).
     *
     * @param samples float PCM audio samples normalized in [-1.0, 1.0]
     * @param offset starting index
     * @param length number of samples
     * @return energy level in dB FS (returns MIN_DB_FS for silence/zero)
     */
    public static float calculateRmsDb(@NonNull float[] samples, int offset, int length) {
        if (samples == null || length <= 0 || offset < 0 || offset + length > samples.length) {
            return MIN_DB_FS;
        }

        double sumSquare = 0.0;
        for (int i = offset; i < offset + length; i++) {
            double s = samples[i];
            sumSquare += s * s;
        }

        double meanSquare = sumSquare / length;
        double rms = Math.sqrt(meanSquare);

        if (rms < 1e-5) { // Below -100 dB FS
            return MIN_DB_FS;
        }

        return (float) (20.0 * Math.log10(rms));
    }

    @Override
    public synchronized boolean isSpeechDetected() {
        return currentState == VadState.SPEECH;
    }

    @Override
    @NonNull
    public synchronized VadState getState() {
        return currentState;
    }

    @Override
    public synchronized void setListener(@Nullable VoiceActivityListener listener) {
        this.listener = listener;
    }

    @Override
    public synchronized void reset() {
        currentState = VadState.SILENCE;
        bufferedCount = 0;
        consecutiveSpeechMs = 0L;
        consecutiveSilenceMs = 0L;
        Arrays.fill(sampleBuffer, 0.0f);
    }

    /**
     * Gets the configured sample rate in Hz.
     *
     * @return sample rate in Hz
     */
    public int getSampleRate() {
        return sampleRate;
    }

    /**
     * Gets the configured frame size in milliseconds.
     *
     * @return frame size in ms
     */
    public int getFrameSizeMs() {
        return frameSizeMs;
    }

    /**
     * Gets the configured energy threshold in dB FS.
     *
     * @return energy threshold in dB FS
     */
    public float getEnergyThresholdDb() {
        return energyThresholdDb;
    }

    /**
     * Gets the configured silence hangover duration in milliseconds.
     *
     * @return silence hangover duration in ms
     */
    public long getSilenceHangoverMs() {
        return silenceHangoverMs;
    }

    /**
     * Gets the configured minimum speech duration in milliseconds required to trigger speech start.
     *
     * @return minimum speech duration in ms
     */
    public long getMinSpeechDurationMs() {
        return minSpeechDurationMs;
    }

    /**
     * Builder for configuring and instantiating {@link EnergyVoiceActivityDetector}.
     */
    public static class Builder {
        private int sampleRate = DEFAULT_SAMPLE_RATE;
        private int frameSizeMs = DEFAULT_FRAME_SIZE_MS;
        private float energyThresholdDb = DEFAULT_ENERGY_THRESHOLD_DB;
        private long silenceHangoverMs = DEFAULT_SILENCE_HANGOVER_MS;
        private long minSpeechDurationMs = DEFAULT_MIN_SPEECH_DURATION_MS;

        public Builder setSampleRate(int sampleRate) {
            if (sampleRate <= 0) {
                throw new IllegalArgumentException("Sample rate must be positive");
            }
            this.sampleRate = sampleRate;
            return this;
        }

        public Builder setFrameSizeMs(int frameSizeMs) {
            if (frameSizeMs <= 0) {
                throw new IllegalArgumentException("Frame size must be positive");
            }
            this.frameSizeMs = frameSizeMs;
            return this;
        }

        public Builder setEnergyThresholdDb(float energyThresholdDb) {
            this.energyThresholdDb = energyThresholdDb;
            return this;
        }

        public Builder setSilenceHangoverMs(long silenceHangoverMs) {
            if (silenceHangoverMs < 0) {
                throw new IllegalArgumentException("Silence hangover duration cannot be negative");
            }
            this.silenceHangoverMs = silenceHangoverMs;
            return this;
        }

        public Builder setMinSpeechDurationMs(long minSpeechDurationMs) {
            if (minSpeechDurationMs < 0) {
                throw new IllegalArgumentException("Minimum speech duration cannot be negative");
            }
            this.minSpeechDurationMs = minSpeechDurationMs;
            return this;
        }

        public EnergyVoiceActivityDetector build() {
            return new EnergyVoiceActivityDetector(this);
        }
    }
}
