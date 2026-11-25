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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link AudioRecorder}.
 *
 * @author Dirk Schnelle-Walka
 */
public class AudioRecorderTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void defaultTarget_isMemory() {
        AudioRecorder recorder = new AudioRecorder();
        assertEquals(AudioRecorder.RecordTarget.MEMORY, recorder.getRecordTarget());
        assertFalse(recorder.isRecording());
        assertNull(recorder.getOutputFile());
    }

    @Test
    public void getPcmInputStream_beforeRecording_returnsEmptyStream() throws IOException {
        AudioRecorder recorder = new AudioRecorder();
        InputStream stream = recorder.getPcmInputStream();
        assertNotNull(stream);
        assertEquals(0, stream.available());
    }

    @Test
    public void getFloatInputStream_beforeRecording_returnsEmptyStream() throws IOException {
        AudioRecorder recorder = new AudioRecorder();
        InputStream stream = recorder.getFloatInputStream();
        assertNotNull(stream);
        assertEquals(-1, stream.read());
    }

    @Test
    public void getOutputFile_whenFileTarget_returnsTargetFile() {
        File file = new File(temporaryFolder.getRoot(), "test.pcm");
        AudioRecorder recorder = new AudioRecorder();
        try {
            recorder.startRecording(file);
        } catch (Exception ignored) {
            // AudioRecord requires Android context / permissions in real run, but state is updated
        }
        assertEquals(AudioRecorder.RecordTarget.FILE, recorder.getRecordTarget());
        assertEquals(file, recorder.getOutputFile());
        recorder.stopRecording();
    }
}
