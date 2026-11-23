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

import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link EnergyVoiceActivityDetector}.
 *
 * @author Dirk Schnelle-Walka
 */
public class EnergyVoiceActivityDetectorTest {

    private static final float EPSILON = 1e-2f;

    @Test
    public void testCalculateRmsDb_silence() {
        float[] silence = new float[320];
        float rmsDb = EnergyVoiceActivityDetector.calculateRmsDb(silence, 0, silence.length);
        assertEquals(EnergyVoiceActivityDetector.MIN_DB_FS, rmsDb, EPSILON);
    }

    @Test
    public void testCalculateRmsDb_fullScaleSquareWave() {
        float[] squareWave = new float[320];
        for (int i = 0; i < squareWave.length; i++) {
            squareWave[i] = (i % 2 == 0) ? 1.0f : -1.0f;
        }
        float rmsDb = EnergyVoiceActivityDetector.calculateRmsDb(squareWave, 0, squareWave.length);
        assertEquals(0.0f, rmsDb, EPSILON);
    }

    @Test
    public void testCalculateRmsDb_sineWave() {
        float[] sineWave = new float[1600];
        double frequency = 440.0;
        int sampleRate = 16000;
        for (int i = 0; i < sineWave.length; i++) {
            sineWave[i] = (float) Math.sin(2.0 * Math.PI * frequency * i / sampleRate);
        }
        float rmsDb = EnergyVoiceActivityDetector.calculateRmsDb(sineWave, 0, sineWave.length);
        // Peak 1.0 sine wave RMS is 1/sqrt(2) approx -3.01 dB FS
        assertEquals(-3.01f, rmsDb, 0.1f);
    }

    @Test
    public void testDefaultBuilderAndGetters() {
        EnergyVoiceActivityDetector detector = new EnergyVoiceActivityDetector();
        assertEquals(EnergyVoiceActivityDetector.DEFAULT_SAMPLE_RATE, detector.getSampleRate());
        assertEquals(EnergyVoiceActivityDetector.DEFAULT_FRAME_SIZE_MS, detector.getFrameSizeMs());
        assertEquals(EnergyVoiceActivityDetector.DEFAULT_ENERGY_THRESHOLD_DB, detector.getEnergyThresholdDb(), EPSILON);
        assertEquals(EnergyVoiceActivityDetector.DEFAULT_SILENCE_HANGOVER_MS, detector.getSilenceHangoverMs());
        assertEquals(EnergyVoiceActivityDetector.DEFAULT_MIN_SPEECH_DURATION_MS, detector.getMinSpeechDurationMs());
        assertEquals(VadState.SILENCE, detector.getState());
        assertFalse(detector.isSpeechDetected());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderInvalidSampleRate() {
        new EnergyVoiceActivityDetector.Builder().setSampleRate(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderInvalidFrameSize() {
        new EnergyVoiceActivityDetector.Builder().setFrameSizeMs(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderInvalidSilenceHangover() {
        new EnergyVoiceActivityDetector.Builder().setSilenceHangoverMs(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderInvalidMinSpeechDuration() {
        new EnergyVoiceActivityDetector.Builder().setMinSpeechDurationMs(-1);
    }

    @Test
    public void testStateTransitions_speechStartAndHangoverEnd() {
        TestVoiceActivityListener listener = new TestVoiceActivityListener();
        EnergyVoiceActivityDetector detector = new EnergyVoiceActivityDetector.Builder()
                .setSampleRate(16000)
                .setFrameSizeMs(20) // 320 samples per frame
                .setEnergyThresholdDb(-30.0f)
                .setMinSpeechDurationMs(40) // 2 frames needed to trigger speech start
                .setSilenceHangoverMs(60) // 3 frames of silence needed to trigger speech end
                .build();

        detector.setListener(listener);

        float[] silenceFrame = new float[320];
        float[] speechFrame = new float[320];
        java.util.Arrays.fill(speechFrame, 0.5f); // Loud constant signal (~ -6 dB FS)

        // 1. Process 2 silence frames
        detector.process(silenceFrame);
        detector.process(silenceFrame);
        assertEquals(VadState.SILENCE, detector.getState());
        assertFalse(detector.isSpeechDetected());
        assertEquals(0, listener.speechStartedCount);

        // 2. Process 1st speech frame (accumulates 20ms < 40ms min duration)
        detector.process(speechFrame);
        assertEquals(VadState.SILENCE, detector.getState());
        assertEquals(0, listener.speechStartedCount);

        // 3. Process 2nd speech frame (accumulates 40ms >= 40ms -> SPEECH started!)
        detector.process(speechFrame);
        assertEquals(VadState.SPEECH, detector.getState());
        assertTrue(detector.isSpeechDetected());
        assertEquals(1, listener.speechStartedCount);
        assertEquals(0, listener.speechEndedCount);

        // 4. Process 2 silence frames (accumulates 40ms < 60ms hangover)
        detector.process(silenceFrame);
        detector.process(silenceFrame);
        assertEquals(VadState.SPEECH, detector.getState());
        assertEquals(0, listener.speechEndedCount);

        // 5. Process 3rd silence frame (accumulates 60ms >= 60ms hangover -> SPEECH ended!)
        detector.process(silenceFrame);
        assertEquals(VadState.SILENCE, detector.getState());
        assertFalse(detector.isSpeechDetected());
        assertEquals(1, listener.speechEndedCount);
        assertFalse(listener.rmsHistory.isEmpty());
    }

    @Test
    public void testProcessPcm16() {
        TestVoiceActivityListener listener = new TestVoiceActivityListener();
        EnergyVoiceActivityDetector detector = new EnergyVoiceActivityDetector.Builder()
                .setSampleRate(16000)
                .setFrameSizeMs(20) // 320 samples = 640 bytes per frame
                .setEnergyThresholdDb(-30.0f)
                .setMinSpeechDurationMs(20)
                .setSilenceHangoverMs(40)
                .build();
        detector.setListener(listener);

        byte[] speechPcm16 = new byte[640];
        ByteBuffer bb = ByteBuffer.wrap(speechPcm16).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < 320; i++) {
            bb.putShort((short) 16384); // 0.5 float equivalent
        }

        detector.processPcm16(speechPcm16);
        assertEquals(VadState.SPEECH, detector.getState());
        assertEquals(1, listener.speechStartedCount);

        detector.reset();
        assertEquals(VadState.SILENCE, detector.getState());
        assertFalse(detector.isSpeechDetected());
    }

    private static class TestVoiceActivityListener implements VoiceActivityListener {
        int speechStartedCount = 0;
        int speechEndedCount = 0;
        List<Float> rmsHistory = new ArrayList<>();

        @Override
        public void onSpeechStarted() {
            speechStartedCount++;
        }

        @Override
        public void onSpeechEnded() {
            speechEndedCount++;
        }

        @Override
        public void onRmsChanged(float rmsDb) {
            rmsHistory.add(rmsDb);
        }
    }
}
