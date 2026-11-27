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

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Audio conversion utilities for Whisper's 16 kHz mono float PCM input.
 *
 * @author Dirk Schnelle-Walka
 */
public final class AudioUtils {

    /** Target sample rate required by Whisper (16 kHz). */
    public static final int TARGET_SAMPLE_RATE = 16_000;

    private AudioUtils() {}

    /**
     * Reads a little-endian PCM16 WAV file.
     * Supports ordinary RIFF/WAVE PCM files with 16-bit integer samples.
     * Other encodings (float WAV, compressed WAV, 24-bit PCM) are rejected.
     *
     * @param file the WAV file to read
     * @return mono float PCM samples at 16 kHz
     * @throws IOException if the file cannot be read or has an unsupported format
     */
    @NonNull
    public static float[] readWav(@NonNull File file) throws IOException {
        byte[] bytes = readAllBytes(file);
        WavHeader header = parseWavHeader(bytes);

        int frameCount = header.dataSize / (header.channels * 2);
        long targetLength = Math.round((double) frameCount * TARGET_SAMPLE_RATE / header.sampleRate);
        float[] outSamples = new float[(int) targetLength];
        int written = fromPcm16(bytes, header.dataOffset, header.dataSize, outSamples, 0, header.sampleRate, header.channels);
        if (written < outSamples.length) {
            return Arrays.copyOf(outSamples, written);
        }
        return outSamples;
    }

    private static byte[] readAllBytes(File file) throws IOException {
        try (InputStream in = new FileInputStream(file);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        }
    }

    private static WavHeader parseWavHeader(byte[] bytes) throws IOException {
        if (bytes.length < 44 ||
                notEqualsAscii(bytes, 0, "RIFF") ||
                notEqualsAscii(bytes, 8, "WAVE")) {
            throw new IOException("Invalid RIFF/WAVE file");
        }

        ByteBuffer header = ByteBuffer.wrap(bytes)
                .order(ByteOrder.LITTLE_ENDIAN);

        int offset = 12;
        int format = -1, channels = -1, sampleRate = -1;
        int bitsPerSample = -1, dataOffset = -1, dataSize = -1;

        while (offset + 8 <= bytes.length) {
            String id = new String(bytes, offset, 4,
                    java.nio.charset.StandardCharsets.US_ASCII);
            long chunkSize = Integer.toUnsignedLong(
                    header.getInt(offset + 4));
            long end = (long) offset + 8 + chunkSize;

            if (end > bytes.length) {
                throw new IOException("Truncated WAV chunk: " + id);
            }

            int body = offset + 8;
            if ("fmt ".equals(id)) {
                if (chunkSize < 16) {
                    throw new IOException("Invalid WAV fmt chunk");
                }
                format = header.getShort(body) & 0xffff;
                channels = header.getShort(body + 2) & 0xffff;
                sampleRate = header.getInt(body + 4);
                bitsPerSample = header.getShort(body + 14) & 0xffff;
            } else if ("data".equals(id)) {
                dataOffset = body;
                dataSize = (int) chunkSize;
            }

            offset = (int) (end + (chunkSize & 1));
        }

        if (format != 1 || channels < 1 || sampleRate <= 0
                || bitsPerSample != 16 || dataOffset < 0) {
            throw new IOException(
                    "Only PCM16 integer WAV files are supported");
        }

        if (dataSize % (channels * 2) != 0) {
            throw new IOException("Incomplete PCM audio frame");
        }

        return new WavHeader(format, channels, sampleRate, bitsPerSample, dataOffset, dataSize);
    }

    private record WavHeader(
            int format,
            int channels,
            int sampleRate,
            int bitsPerSample,
            int dataOffset,
            int dataSize) {}

    /**
     * Converts a slice of interleaved signed PCM16 little-endian bytes to 16 kHz mono float PCM.
     * Default assumption: 16 kHz sample rate, 1 channel (mono).
     *
     * @param pcm interleaved PCM byte buffer
     * @param offset byte offset in pcm array
     * @param length number of PCM bytes to convert
     * @param out destination float array
     * @param outOffset starting index in out array
     * @return number of float samples written to out
     */
    public static int fromPcm16(
            @NonNull byte[] pcm, int offset, int length,
            @NonNull float[] out, int outOffset) {
        return fromPcm16(pcm, offset, length, out, outOffset, TARGET_SAMPLE_RATE, 1);
    }

    /**
     * Converts a slice of interleaved signed PCM16 little-endian bytes to 16 kHz mono float PCM.
     *
     * @param pcm interleaved PCM byte buffer
     * @param offset byte offset in pcm array
     * @param length number of PCM bytes to convert
     * @param out destination float array
     * @param outOffset starting index in out array
     * @param sourceRate source sample rate in Hz
     * @param channels number of interleaved channels
     * @return number of float samples written to out
     */
    @SuppressWarnings("ConstantConditions")
    public static int fromPcm16(
            @NonNull byte[] pcm, int offset, int length,
            @NonNull float[] out, int outOffset,
            int sourceRate, int channels) {
        if (pcm == null) {
            throw new IllegalArgumentException("pcm == null");
        }
        if (out == null) {
            throw new IllegalArgumentException("out == null");
        }
        if (sourceRate <= 0 || channels <= 0) {
            throw new IllegalArgumentException(
                    "Sample rate and channel count must be positive");
        }
        if (offset < 0 || length < 0 || offset + length > pcm.length) {
            throw new IllegalArgumentException("Invalid offset or length bounds");
        }
        if (length % (channels * 2) != 0) {
            throw new IllegalArgumentException(
                    "PCM buffer contains an incomplete frame");
        }

        int frames = length / (channels * 2);
        float[] mono = new float[frames];

        ByteBuffer input = ByteBuffer.wrap(pcm, offset, length)
                .order(ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < frames; i++) {
            float sum = 0f;
            for (int c = 0; c < channels; c++) {
                sum += input.getShort() / 32768.0f;
            }
            mono[i] = clamp(sum / channels);
        }

        float[] resampled = resample(mono, sourceRate, TARGET_SAMPLE_RATE);
        if (outOffset < 0 || outOffset + resampled.length > out.length) {
            throw new IllegalArgumentException("Destination array out buffer is too small");
        }
        System.arraycopy(resampled, 0, out, outOffset, resampled.length);
        return resampled.length;
    }

    private static boolean notEqualsAscii(byte[] b, int offset, String text) {
        if (offset + text.length() > b.length) return true;
        for (int i = 0; i < text.length(); i++) {
            if (b[offset + i] != (byte) text.charAt(i)) return true;
        }
        return false;
    }

    /**
     * Linear resampling. For high-quality production capture pipelines,
     * replace with a band-limited resampler to reduce aliasing.
     *
     * @param input input audio samples
     * @param sourceRate source sample rate in Hz
     * @param targetRate target sample rate in Hz
     * @return resampled audio float array
     */
    @NonNull
    @SuppressWarnings("ConstantConditions")
    public static float[] resample(
            @NonNull float[] input, int sourceRate, int targetRate) {
        if (input == null) {
            throw new IllegalArgumentException("input == null");
        }
        if (sourceRate <= 0 || targetRate <= 0) {
            throw new IllegalArgumentException("Invalid sample rate");
        }
        if (input.length == 0) return new float[0];
        if (sourceRate == targetRate) return input.clone();

        long targetLength = Math.round(
                (double) input.length * targetRate / sourceRate);
        if (targetLength > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Audio buffer is too large");
        }

        int n = (int) targetLength;
        float[] output = new float[n];

        for (int i = 0; i < n; i++) {
            double position = (double) i * sourceRate / targetRate;
            int left = Math.min((int) position, input.length - 1);
            int right = Math.min(left + 1, input.length - 1);
            float fraction = (float) (position - left);

            output[i] = clamp(
                    input[left] * (1f - fraction)
                            + input[right] * fraction);
        }

        return output;
    }

    private static float clamp(float value) {
        return Math.max(-1f, Math.min(1f, value));
    }
}
