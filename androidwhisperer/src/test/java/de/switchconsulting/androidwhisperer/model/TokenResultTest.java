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

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TokenResultTest {

    @Test
    public void tokenResult_valid() {
        TokenResult token = new TokenResult("Hello", 100, 300);
        assertEquals("Hello", token.getText());
        assertEquals(100, token.getStartMs());
        assertEquals(300, token.getEndMs());
        assertEquals(200, token.getDurationMs());
    }

    @Test
    public void tokenResult_invalidInterval_throwsException() {
        try {
            new TokenResult("Test", 300, 100);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Invalid token interval", e.getMessage());
        }
    }

    @Test
    public void tokenResult_toJSON() throws Exception {
        TokenResult token = new TokenResult("Hello", 100, 300);
        JSONObject json = token.toJSON();
        assertEquals("Hello", json.getString("text"));
        assertEquals(100, json.getLong("startMs"));
        assertEquals(300, json.getLong("endMs"));
        assertEquals(200, json.getLong("durationMs"));
    }
}
