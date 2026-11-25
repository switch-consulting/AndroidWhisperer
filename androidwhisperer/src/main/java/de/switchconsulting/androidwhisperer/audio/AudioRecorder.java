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

package de.switchconsulting.androidwhisperer.audio;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * General-purpose Android audio recorder supporting dual output targets:
 * physical files on disk or in-memory buffers in RAM.
 * <p>
 * Provides stream-based accessors to retrieve recorded audio as either raw 16-bit PCM bytes
 * or 32-bit IEEE 754 floating-point audio samples at 16 kHz mono.
 * </p>
 *
 * @author Dirk Schnelle-Walka
 */
public class AudioRecorder {

    private static final String TAG = "AudioRecorder";
    public static final int SAMPLE_RATE = 16_000;
    public static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    /** Destination target for recorded audio. */
    public enum RecordTarget {
        /** Audio is recorded directly into an in-memory RAM byte array (zero disk I/O). */
        MEMORY,
        /** Audio is recorded into a physical File on disk. */
        FILE
    }

    private AudioRecord audioRecord;
    private Thread recordingThread;
    private volatile boolean isRecording = false;

    private RecordTarget target = RecordTarget.MEMORY;
    private File outputFile;
    private ByteArrayOutputStream memoryOutputStream;

    /** Constructs a new AudioRecorder instance. */
    public AudioRecorder() {
    }

    /**
     * Starts recording audio into an in-memory buffer (RAM).
     *
     * @throws IOException if AudioRecord fails to initialize
     */
    public synchronized void startRecording() throws IOException {
        startRecordingInternal(RecordTarget.MEMORY, null);
    }

    /**
     * Starts recording audio into a specified physical file.
     *
     * @param outputFile target file where PCM data will be written
     * @throws IOException if AudioRecord fails to initialize or file cannot be prepared
     */
    public synchronized void startRecording(@NonNull File outputFile) throws IOException {
        startRecordingInternal(RecordTarget.FILE, outputFile);
    }

    /**
     * Starts recording audio into a file within the specified directory.
     *
     * @param outputDir directory to store the recorded file
     * @param fileName file name for the recorded file
     * @throws IOException if AudioRecord fails to initialize or file cannot be prepared
     */
    public synchronized void startRecording(@NonNull File outputDir, @NonNull String fileName) throws IOException {
        File targetFile = new File(outputDir, fileName);
        startRecording(targetFile);
    }

    @SuppressLint({"MissingPermission", "ResultOfMethodCallIgnored"})
    private void startRecordingInternal(RecordTarget recordTarget, @Nullable File file) throws IOException {
        if (isRecording) {
            return;
        }

        this.target = recordTarget;
        if (target == RecordTarget.FILE) {
            this.outputFile = file;
            if (outputFile != null && outputFile.exists()) {
                outputFile.delete();
            }
            this.memoryOutputStream = null;
        } else {
            this.outputFile = null;
            this.memoryOutputStream = new ByteArrayOutputStream();
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

        recordingThread = new Thread(this::writeAudioData, "AudioRecorder Thread");
        recordingThread.start();
        Log.d(TAG, "Recording started (Target: " + target + ").");
    }

    private void writeAudioData() {
        byte[] data = new byte[512];

        try {
            if (target == RecordTarget.FILE && outputFile != null) {
                try (FileOutputStream os = new FileOutputStream(outputFile)) {
                    writeToStream(os, data);
                }
            } else if (memoryOutputStream != null) {
                writeToStream(memoryOutputStream, data);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error writing audio data", e);
        }
    }

    private void writeToStream(OutputStream os, byte[] data) throws IOException {
        while (isRecording) {
            int read = audioRecord.read(data, 0, data.length);
            if (read > 0) {
                os.write(data, 0, read);
            }
        }
    }

    /**
     * Stops the audio recording.
     */
    public synchronized void stopRecording() {
        if (!isRecording) {
            return;
        }

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

        Log.d(TAG, "Recording stopped.");
    }

    /** Checks whether recording is currently active. */
    public boolean isRecording() {
        return isRecording;
    }

    /** Returns the active {@link RecordTarget}. */
    @NonNull
    public RecordTarget getRecordTarget() {
        return target;
    }

    /**
     * Returns the output file if recorded using {@link RecordTarget#FILE}, or null if recorded in memory.
     */
    @Nullable
    public File getOutputFile() {
        return outputFile;
    }

    /**
     * Returns an {@link InputStream} containing raw 16-bit signed integer PCM audio frames
     * encoded in little-endian byte order at 16,000 Hz single-channel (mono).
     *
     * @return InputStream of PCM16 audio bytes
     * @throws IOException if obtaining the stream fails
     */
    @NonNull
    public InputStream getPcmInputStream() throws IOException {
        if (target == RecordTarget.FILE) {
            if (outputFile == null || !outputFile.exists()) {
                throw new IOException("Recorded PCM file does not exist.");
            }
            return new FileInputStream(outputFile);
        } else {
            byte[] bytes = (memoryOutputStream != null) ? memoryOutputStream.toByteArray() : new byte[0];
            return new ByteArrayInputStream(bytes);
        }
    }

    /**
     * Returns an {@link InputStream} containing 32-bit IEEE 754 floating-point audio samples
     * encoded in little-endian byte order at 16,000 Hz single-channel (mono) in the range [-1.0, 1.0].
     *
     * @return InputStream of 32-bit float audio bytes
     * @throws IOException if obtaining the stream fails
     */
    @NonNull
    public InputStream getFloatInputStream() throws IOException {
        return new PcmToFloatInputStream(getPcmInputStream());
    }

    /**
     * Wraps a PCM 16-bit little-endian stream and produces a 32-bit IEEE 754 float little-endian stream.
     */
    private static class PcmToFloatInputStream extends InputStream {
        private final InputStream sourcePcm;
        private final byte[] floatBuffer = new byte[4];
        private int floatBufferPos = 4; // empty initially

        PcmToFloatInputStream(InputStream sourcePcm) {
            this.sourcePcm = sourcePcm;
        }

        @Override
        public int read() throws IOException {
            if (floatBufferPos < 4) {
                return floatBuffer[floatBufferPos++] & 0xFF;
            }

            int b1 = sourcePcm.read();
            if (b1 == -1) return -1;
            int b2 = sourcePcm.read();
            if (b2 == -1) return -1;

            short sample = (short) ((b2 << 8) | (b1 & 0xFF));
            float floatVal = Math.max(-1.0f, Math.min(1.0f, sample / 32768.0f));
            int bits = Float.floatToIntBits(floatVal);

            floatBuffer[0] = (byte) (bits & 0xFF);
            floatBuffer[1] = (byte) ((bits >> 8) & 0xFF);
            floatBuffer[2] = (byte) ((bits >> 16) & 0xFF);
            floatBuffer[3] = (byte) ((bits >> 24) & 0xFF);

            floatBufferPos = 1;
            return floatBuffer[0] & 0xFF;
        }

        @Override
        public int read(@NonNull byte[] b, int off, int len) throws IOException {
            if (off < 0 || len < 0 || len > b.length - off) throw new IndexOutOfBoundsException();
            if (len == 0) return 0;

            int count = 0;
            while (count < len) {
                int val = read();
                if (val == -1) {
                    return count == 0 ? -1 : count;
                }
                b[off + count] = (byte) val;
                count++;
            }
            return count;
        }

        @Override
        public void close() throws IOException {
            sourcePcm.close();
            super.close();
        }
    }
}
