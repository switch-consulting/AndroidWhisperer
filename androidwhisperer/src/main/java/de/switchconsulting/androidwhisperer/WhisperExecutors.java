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

import androidx.annotation.NonNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Internal utility providing shared background executors across AndroidWhisperer.
 */
public final class WhisperExecutors {

    private static final ExecutorService DEFAULT_EXECUTOR = Executors.newFixedThreadPool(2, new ThreadFactory() {
        private final AtomicInteger counter = new AtomicInteger(1);

        @Override
        public Thread newThread(@NonNull Runnable r) {
            Thread t = new Thread(r, "WhisperBackground-" + counter.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    });

    private WhisperExecutors() {
        // Utility class
    }

    /**
     * Returns the shared default background executor service for AndroidWhisperer.
     */
    @NonNull
    public static ExecutorService getDefaultExecutor() {
        return DEFAULT_EXECUTOR;
    }
}
