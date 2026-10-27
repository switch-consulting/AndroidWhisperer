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

package de.switchconsulting.androidwhisperer.model;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class SegmentResultTest {

    @Test
    public void segmentResult_valid() {
        PauseSegment pause = new PauseSegment(50, 100, PauseSegment.Level.TOKEN);
        SegmentResult segment = new SegmentResult("Hello", 0, 200, Collections.singletonList(pause));
        assertEquals("Hello", segment.getText());
        assertEquals(0, segment.getStartMs());
        assertEquals(200, segment.getEndMs());
        assertEquals(1, segment.getPauses().size());

        SegmentResult nullSafe = new SegmentResult(null, 0, 100, null);
        assertEquals("", nullSafe.getText());
        assertNotNull(nullSafe.getPauses());
    }

    @Test
    public void segmentResult_invalidInterval_throwsException() {
        try {
            new SegmentResult("Test", 200, 100, Collections.emptyList());
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Invalid segment interval", e.getMessage());
        }
    }
}
