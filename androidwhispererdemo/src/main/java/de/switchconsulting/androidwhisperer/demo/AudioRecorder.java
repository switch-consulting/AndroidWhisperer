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

package de.switchconsulting.androidwhisperer.demo;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class AudioRecorder {
    private static final String TAG = "AudioRecorder";
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord audioRecord;
    private Thread recordingThread;
    private boolean isRecording = false;
    private File outputFile;

    @SuppressLint({"MissingPermission", "ResultOfMethodCallIgnored"})
    public void startRecording(File outputDir) throws IOException {
        if (isRecording) return;

        outputFile = new File(outputDir, "recorded_audio.pcm");
        if (outputFile.exists()) {
            boolean ignored = outputFile.delete();
        }

        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            bufferSize = SAMPLE_RATE * 2;
        }

        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize * 2
        );

        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            throw new IOException("AudioRecord initialization failed.");
        }

        audioRecord.startRecording();
        isRecording = true;

        recordingThread = new Thread(this::writeAudioDataToFile, "AudioRecorder Thread");

        recordingThread.start();
        Log.d(TAG, "Recording started.");
    }

    private void writeAudioDataToFile() {
        byte[] data = new byte[512];

        try (FileOutputStream os = new FileOutputStream(outputFile)) {
            while (isRecording) {
                int read = audioRecord.read(data, 0, data.length);
                if (read > 0) {
                    os.write(data, 0, read);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error writing audio data", e);
        }
    }

    public File stopRecording() {
        if (!isRecording) return null;

        isRecording = false;
        if (audioRecord != null) {
            try {
                audioRecord.stop();
                audioRecord.release();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping AudioRecord", e);
            }
            audioRecord = null;
        }

        if (recordingThread != null) {
            try {
                recordingThread.join();
            } catch (InterruptedException e) {
                Log.e(TAG, "Recording thread interrupted", e);
            }
            recordingThread = null;
        }

        Log.d(TAG, "Recording stopped. File path: " + (outputFile != null ? outputFile.getAbsolutePath() : "null"));
        return outputFile;
    }

    @SuppressWarnings("unused")
    public boolean isRecording() {
        return isRecording;
    }

    public static float[] convertPcmToFloat(File pcmFile) throws IOException {
        int fileSize = (int) pcmFile.length();
        byte[] pcmBytes = new byte[fileSize];
        try (FileInputStream fis = new FileInputStream(pcmFile)) {
            int bytesRead = 0;
            while (bytesRead < fileSize) {
                int read = fis.read(pcmBytes, bytesRead, fileSize - bytesRead);
                if (read < 0) break;
                bytesRead += read;
            }
        }

        int numSamples = fileSize / 2;
        float[] floatSamples = new float[numSamples];
        for (int i = 0; i < numSamples; i++) {
            int low = pcmBytes[2 * i] & 0xFF;
            int high = pcmBytes[2 * i + 1] & 0xFF;
            int sample = (high << 8) | low;
            if (sample > 32767) {
                sample -= 65536;
            }
            floatSamples[i] = sample / 32768.0f;
        }
        return floatSamples;
    }
}
