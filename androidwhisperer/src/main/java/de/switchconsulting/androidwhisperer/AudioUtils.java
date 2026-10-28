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

package de.switchconsulting.androidwhisperer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/** Audio conversion utilities for Whisper's 16 kHz mono float PCM input. */
public final class AudioUtils {

    public static final int TARGET_SAMPLE_RATE = 16_000;

    private AudioUtils() {}

    /**
     * Reads a little-endian PCM16 WAV file.
     * Supports ordinary RIFF/WAVE PCM files with 16-bit integer samples.
     * Other encodings (float WAV, compressed WAV, 24-bit PCM) are rejected.
     */
    public static float[] readWav(File file) throws IOException {
        byte[] bytes;
        try (InputStream in = new FileInputStream(file);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
            bytes = out.toByteArray();
        }

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

        // WAV chunks may occur in different orders and may include padding.
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

        byte[] pcm = Arrays.copyOfRange(
                bytes, dataOffset, dataOffset + dataSize);

        return fromPcm16(pcm, sampleRate, channels);
    }

    private static boolean notEqualsAscii(byte[] b, int offset, String text) {
        if (offset + text.length() > b.length) return true;
        for (int i = 0; i < text.length(); i++) {
            if (b[offset + i] != (byte) text.charAt(i)) return true;
        }
        return false;
    }

    /**
     * Converts interleaved signed PCM16 little-endian bytes to 16 kHz mono.
     *
     * @param pcm interleaved PCM frames
     * @param sourceRate source sample rate in Hz
     * @param channels number of interleaved channels
     */
    public static float[] fromPcm16(
            byte[] pcm, int sourceRate, int channels) {

        if (pcm == null) throw new IllegalArgumentException("pcm == null");
        if (sourceRate <= 0 || channels <= 0) {
            throw new IllegalArgumentException(
                    "Sample rate and channel count must be positive");
        }
        if (pcm.length % (channels * 2) != 0) {
            throw new IllegalArgumentException(
                    "PCM buffer contains an incomplete frame");
        }

        int frames = pcm.length / (channels * 2);
        float[] mono = new float[frames];

        ByteBuffer input = ByteBuffer.wrap(pcm)
                .order(ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < frames; i++) {
            float sum = 0f;
            for (int c = 0; c < channels; c++) {
                sum += input.getShort() / 32768.0f;
            }
            mono[i] = clamp(sum / channels);
        }

        return resample(mono, sourceRate, TARGET_SAMPLE_RATE);
    }

    /**
     * Converts interleaved float PCM in [-1, 1] to mono 16 kHz.
     * Input must contain one float per channel per frame.
     */
    public static float[] fromFloatPcm(
            float[] interleaved, int sourceRate, int channels) {

        if (interleaved == null) {
            throw new IllegalArgumentException("interleaved == null");
        }
        if (sourceRate <= 0 || channels <= 0
                || interleaved.length % channels != 0) {
            throw new IllegalArgumentException("Invalid audio format");
        }

        int frames = interleaved.length / channels;
        float[] mono = new float[frames];

        for (int i = 0; i < frames; i++) {
            float sum = 0f;
            for (int c = 0; c < channels; c++) {
                float value = interleaved[i * channels + c];
                if (!Float.isFinite(value)) {
                    throw new IllegalArgumentException(
                            "Audio contains NaN or infinity");
                }
                sum += clamp(value);
            }
            mono[i] = clamp(sum / channels);
        }

        return resample(mono, sourceRate, TARGET_SAMPLE_RATE);
    }

    /**
     * Linear resampling. For high-quality production capture pipelines,
     * replace with a band-limited resampler to reduce aliasing.
     */
    public static float[] resample(
            float[] input, int sourceRate, int targetRate) {

        if (input == null) throw new IllegalArgumentException("input == null");
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