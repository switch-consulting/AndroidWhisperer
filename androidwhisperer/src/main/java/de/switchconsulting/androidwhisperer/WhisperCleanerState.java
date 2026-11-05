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

import com.sun.jna.Pointer;
import de.switchconsulting.androidwhisperer.internal.WhisperBridge;

/**
 * Reusable cleanup action state for native whisper contexts via {@link java.lang.ref.Cleaner}.
 */
final class WhisperCleanerState implements Runnable {
    private final WhisperBridge nativeApi;
    Pointer context;

    WhisperCleanerState(WhisperBridge nativeApi, Pointer context) {
        this.nativeApi = nativeApi;
        this.context = context;
    }

    @Override
    public void run() {
        Pointer ptr = context;
        context = null;
        if (ptr != null && Pointer.nativeValue(ptr) != 0) {
            try {
                nativeApi.whisper_free(ptr);
            } catch (Throwable ignored) {
            }
        }
    }
}
