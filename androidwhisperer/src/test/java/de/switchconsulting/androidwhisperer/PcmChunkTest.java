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
import static org.junit.Assert.assertNotEquals;

/**
 * Unit tests for {@link PcmChunk}.
 *
 * @author Dirk Schnelle-Walka
 */
public class PcmChunkTest {

    @Test
    public void pcmChunk_fieldsAndGetters() {
        PcmChunk chunk = new PcmChunk(4096, 0xAB);
        assertEquals(4096, chunk.totalBytes());
        assertEquals(0xAB, chunk.leftoverByte());
    }

    @Test
    public void pcmChunk_equalsAndHashCode() {
        PcmChunk chunk1 = new PcmChunk(1024, -1);
        PcmChunk chunk2 = new PcmChunk(1024, -1);
        PcmChunk chunk3 = new PcmChunk(2048, -1);

        assertEquals(chunk1, chunk2);
        assertEquals(chunk1.hashCode(), chunk2.hashCode());
        assertNotEquals(chunk1, chunk3);
    }
}
