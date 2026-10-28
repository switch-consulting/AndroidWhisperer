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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PauseModeTest {

    @Test
    public void pauseMode_values() {
        PauseMode[] values = PauseMode.values();
        assertEquals(4, values.length);
        assertEquals(PauseMode.NONE, PauseMode.valueOf("NONE"));
        assertEquals(PauseMode.SEGMENT, PauseMode.valueOf("SEGMENT"));
        assertEquals(PauseMode.TOKEN, PauseMode.valueOf("TOKEN"));
        assertEquals(PauseMode.BOTH, PauseMode.valueOf("BOTH"));
    }
}
