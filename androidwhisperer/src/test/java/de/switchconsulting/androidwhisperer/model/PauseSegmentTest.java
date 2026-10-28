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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class PauseSegmentTest {

    @Test
    public void pauseSegment_valid() {
        PauseSegment pause = new PauseSegment(100, 300, PauseLevel.SEGMENT);
        assertEquals(100, pause.getStartMs());
        assertEquals(300, pause.getEndMs());
        assertEquals(200, pause.getDurationMs());
        assertEquals(PauseLevel.SEGMENT, pause.getLevel());
    }

    @Test
    public void pauseSegment_invalidStart_throwsException() {
        try {
            new PauseSegment(-1, 100, PauseLevel.TOKEN);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Invalid pause interval", e.getMessage());
        }
    }

    @Test
    public void pauseSegment_invalidEnd_throwsException() {
        try {
            new PauseSegment(200, 100, PauseLevel.TOKEN);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Invalid pause interval", e.getMessage());
        }
    }

    @Test
    public void pauseSegment_nullLevel_throwsException() {
        try {
            new PauseSegment(0, 100, null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("level == null", e.getMessage());
        }
    }
}
