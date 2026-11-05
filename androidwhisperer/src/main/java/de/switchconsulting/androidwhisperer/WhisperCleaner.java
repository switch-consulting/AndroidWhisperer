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

import java.lang.reflect.Method;

/**
 * Utility for API-safe registration of native memory cleanup actions via {@link java.lang.ref.Cleaner}.
 * Uses reflection to support Java 9+ and Android API 33+ while remaining compatible with older Android versions.
 */
final class WhisperCleaner {

    private static final Object CLEANER;
    private static final Method REGISTER_METHOD;

    static {
        Object cleaner = null;
        Method register = null;
        try {
            Class<?> cleanerClass = Class.forName("java.lang.ref.Cleaner");
            Method createMethod = cleanerClass.getMethod("create");
            cleaner = createMethod.invoke(null);
            register = cleanerClass.getMethod("register", Object.class, Runnable.class);
        } catch (Throwable ignored) {
        }
        CLEANER = cleaner;
        REGISTER_METHOD = register;
    }

    private WhisperCleaner() {
    }

    /**
     * Registers a cleanup action for the given trackable object.
     */
    static Object register(Object obj, Runnable action) {
        if (CLEANER == null || REGISTER_METHOD == null || action == null) {
            return null;
        }
        try {
            return REGISTER_METHOD.invoke(CLEANER, obj, action);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Explicitly triggers cleanup on a registered cleanable handle.
     */
    static void clean(Object cleanable) {
        if (cleanable != null) {
            try {
                Class<?> cleanableInterface = Class.forName("java.lang.ref.Cleaner$Cleanable");
                Method cleanMethod = cleanableInterface.getMethod("clean");
                cleanMethod.invoke(cleanable);
            } catch (Throwable ignored) {
            }
        }
    }
}
