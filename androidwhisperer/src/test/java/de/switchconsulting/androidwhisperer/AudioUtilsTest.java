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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class AudioUtilsTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void resample_nullInput_throwsException() {
        try {
            AudioUtils.resample(null, 16000, 16000);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("input == null", e.getMessage());
        }
    }

    @Test
    public void resample_invalidRates_throwsException() {
        try {
            AudioUtils.resample(new float[]{0f}, 0, 16000);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Invalid sample rate", e.getMessage());
        }

        try {
            AudioUtils.resample(new float[]{0f}, 16000, -1);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Invalid sample rate", e.getMessage());
        }
    }

    @Test
    public void resample_emptyInput_returnsEmpty() {
        float[] res = AudioUtils.resample(new float[0], 16000, 16000);
        assertNotNull(res);
        assertEquals(0, res.length);
    }

    @Test
    public void resample_sameRate_returnsClone() {
        float[] input = new float[]{0.1f, 0.2f, 0.3f};
        float[] res = AudioUtils.resample(input, 16000, 16000);
        assertArrayEquals(input, res, 0.0001f);
    }

    @Test
    public void resample_upAndDown() {
        float[] input = new float[]{0f, 1f, 0f};
        float[] up = AudioUtils.resample(input, 8000, 16000);
        assertNotNull(up);

        float[] down = AudioUtils.resample(up, 16000, 8000);
        assertNotNull(down);
    }

    @Test
    public void fromFloatPcm_nullInput_throwsException() {
        try {
            AudioUtils.fromFloatPcm(null, 16000, 1);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("interleaved == null", e.getMessage());
        }
    }

    @Test
    public void fromFloatPcm_invalidFormat_throwsException() {
        try {
            AudioUtils.fromFloatPcm(new float[]{0.1f, 0.2f}, 16000, 3);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Invalid audio format", e.getMessage());
        }
    }

    @Test
    public void fromFloatPcm_nonFiniteValue_throwsException() {
        try {
            AudioUtils.fromFloatPcm(new float[]{Float.NaN}, 16000, 1);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Audio contains NaN or infinity", e.getMessage());
        }
    }

    @Test
    public void fromFloatPcm_validInput() {
        float[] input = new float[]{0.5f, -0.5f, 1.5f, -1.5f}; // 1.5 and -1.5 will be clamped
        float[] res = AudioUtils.fromFloatPcm(input, 16000, 2);
        assertNotNull(res);
    }

    @Test
    public void fromPcm16_nullInput_throwsException() {
        try {
            AudioUtils.fromPcm16(null, 16000, 1);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("pcm == null", e.getMessage());
        }
    }

    @Test
    public void fromPcm16_invalidSampleRateOrChannels_throwsException() {
        try {
            AudioUtils.fromPcm16(new byte[4], 0, 1);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Sample rate and channel count must be positive", e.getMessage());
        }
    }

    @Test
    public void fromPcm16_incompleteFrame_throwsException() {
        try {
            AudioUtils.fromPcm16(new byte[3], 16000, 1);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("PCM buffer contains an incomplete frame", e.getMessage());
        }
    }

    @Test
    public void fromPcm16_validBytes() {
        byte[] pcm = new byte[]{0, 0, (byte) 0xff, 0x7f}; // two samples
        float[] res = AudioUtils.fromPcm16(pcm, 16000, 1);
        assertNotNull(res);
    }

    @Test
    public void readWav_invalidFile_throwsIOException() throws Exception {
        File invalidFile = temporaryFolder.newFile("invalid.wav");
        Files.write(invalidFile.toPath(), new byte[]{1, 2, 3, 4});

        try {
            AudioUtils.readWav(invalidFile);
            fail("Expected IOException");
        } catch (IOException e) {
            assertEquals("Invalid RIFF/WAVE file", e.getMessage());
        }
    }

    @Test
    public void readWav_validFile() throws Exception {
        File validFile = createValidWavFile(temporaryFolder);
        float[] samples = AudioUtils.readWav(validFile);
        assertNotNull(samples);
    }

    private File createValidWavFile(TemporaryFolder tempFolder) throws IOException {
        File file = tempFolder.newFile("test.wav");
        byte[] pcmData = new byte[320]; // 160 samples * 2 bytes
        byte[] wavBytes = buildWavBytes(pcmData, 16000, 1, 16);
        Files.write(file.toPath(), wavBytes);
        return file;
    }

    private byte[] buildWavBytes(byte[] pcmData, int sampleRate, int channels, int bitsPerSample) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int dataSize = pcmData.length;
        int fileSize = 36 + dataSize;

        // RIFF header
        out.write("RIFF".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        out.write(intToByteArray(fileSize), 0, 4);
        out.write("WAVE".getBytes(java.nio.charset.StandardCharsets.US_ASCII));

        // fmt chunk
        out.write("fmt ".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        out.write(intToByteArray(16), 0, 4);
        out.write(shortToByteArray((short) 1), 0, 2);
        out.write(shortToByteArray((short) channels), 0, 2);
        out.write(intToByteArray(sampleRate), 0, 4);
        int byteRate = sampleRate * channels * (bitsPerSample / 8);
        out.write(intToByteArray(byteRate), 0, 4);
        short blockAlign = (short) (channels * (bitsPerSample / 8));
        out.write(shortToByteArray(blockAlign), 0, 2);
        out.write(shortToByteArray((short) bitsPerSample), 0, 2);

        // data chunk
        out.write("data".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        out.write(intToByteArray(dataSize), 0, 4);
        out.write(pcmData);

        return out.toByteArray();
    }

    private byte[] intToByteArray(int value) {
        return new byte[] {
                (byte)(value & 0xFF),
                (byte)((value >> 8) & 0xFF),
                (byte)((value >> 16) & 0xFF),
                (byte)((value >> 24) & 0xFF)
        };
    }

    private byte[] shortToByteArray(short value) {
        return new byte[] {
                (byte)(value & 0xFF),
                (byte)((value >> 8) & 0xFF)
        };
    }
}
