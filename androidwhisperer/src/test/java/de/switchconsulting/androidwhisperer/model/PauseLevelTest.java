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

public class PauseLevelTest {

    @Test
    public void pauseLevel_values() {
        PauseLevel[] values = PauseLevel.values();
        assertEquals(2, values.length);
        assertEquals(PauseLevel.SEGMENT, PauseLevel.valueOf("SEGMENT"));
        assertEquals(PauseLevel.TOKEN, PauseLevel.valueOf("TOKEN"));
    }
}
