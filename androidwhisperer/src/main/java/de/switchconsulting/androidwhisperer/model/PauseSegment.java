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

/** A detected silent interval between two speech boundaries. */
public final class PauseSegment {

    private final long startMs;
    private final long endMs;
    private final long durationMs;
    private final PauseLevel level;

    public PauseSegment(long startMs, long endMs, PauseLevel level) {
        if (startMs < 0 || endMs < startMs) {
            throw new IllegalArgumentException("Invalid pause interval");
        }
        if (level == null) {
            throw new IllegalArgumentException("level == null");
        }

        this.startMs = startMs;
        this.endMs = endMs;
        this.durationMs = endMs - startMs;
        this.level = level;
    }

    public long getStartMs() { return startMs; }
    public long getEndMs() { return endMs; }
    public long getDurationMs() { return durationMs; }
    public PauseLevel getLevel() { return level; }
}
