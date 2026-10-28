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

import org.junit.Test;

import de.switchconsulting.androidwhisperer.internal.WhisperBridge;
import de.switchconsulting.androidwhisperer.internal.WhisperProgressCallback;
import de.switchconsulting.androidwhisperer.internal.WhisperSegmentCallback;
import de.switchconsulting.androidwhisperer.model.SegmentResult;
import de.switchconsulting.androidwhisperer.model.TranscriptionResult;

import java.io.File;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AndroidWhispererTest {

    @Test(expected = IllegalArgumentException.class)
    public void constructor_nullBridge_throws() {
        new AndroidWhisperer(null);
    }

    @Test
    public void detectLanguage_success() {
        WhisperBridge initBridge = (WhisperBridge) Proxy.newProxyInstance(
                WhisperBridge.class.getClassLoader(),
                new Class<?>[]{WhisperBridge.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("whisper_init_from_file")) {
                        return new Pointer(999L);
                    } else if (name.equals("whisper_bridge_detect_language")) {
                        return 0;
                    } else if (name.equals("whisper_lang_str")) {
                        return "en";
                    } else if (name.equals("whisper_free")) {
                        return null;
                    }
                    return null;
                });

        try (AndroidWhisperer w = new AndroidWhisperer(initBridge)) {
            w.initModelFromFile(new File("dummy.bin"));
            String lang = w.detectLanguage(new float[]{0.1f, 0.2f});
            assertEquals("en", lang);
        }
    }

    @Test
    public void transcribe_successWithPauses() {
        WhisperBridge bridge = (WhisperBridge) Proxy.newProxyInstance(
                WhisperBridge.class.getClassLoader(),
                new Class<?>[]{WhisperBridge.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    switch (name) {
                        case "whisper_init_from_file":
                            return new Pointer(999L);
                        case "whisper_bridge_transcribe":
                            return 0;
                        case "whisper_full_n_segments":
                            return 1;
                        case "whisper_full_get_segment_t0":
                            return 0L;
                        case "whisper_full_get_segment_t1":
                            return 100L;
                        case "whisper_full_get_segment_text":
                            return "Hello world";
                        case "whisper_full_n_tokens":
                            return 2;
                        case "whisper_full_get_token_t0":
                            int tokenIdx = (int) args[2];
                            return tokenIdx == 0 ? 0L : 60L;
                        case "whisper_full_get_token_t1":
                            int tIdx = (int) args[2];
                            return tIdx == 0 ? 30L : 90L;
                        case "whisper_full_get_token_text":
                            int ti = (int) args[2];
                            return ti == 0 ? " Hello" : " world";
                        case "whisper_bridge_detect_language":
                            return 0;
                        case "whisper_lang_str":
                            return "en";
                        case "whisper_free":
                            return null;
                        default:
                            return null;
                    }
                });

        try (AndroidWhisperer w = new AndroidWhisperer(bridge)) {
            w.initModelFromFile(new File("dummy.bin"));
            TranscriptionResult result = w.transcribe(new float[]{0.1f}, AndroidWhispererOptions.defaults());
            assertNotNull(result);
            assertEquals("Hello world", result.getText());
            assertEquals("en", result.getLanguage());
            assertEquals(1, result.getSegments().size());
        }
    }

    @Test
    public void transcribe_withListener_invokesCallbacks() {
        WhisperBridge bridge = (WhisperBridge) Proxy.newProxyInstance(
                WhisperBridge.class.getClassLoader(),
                new Class<?>[]{WhisperBridge.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    switch (name) {
                        case "whisper_init_from_file":
                            return new Pointer(999L);
                        case "whisper_bridge_transcribe_with_callback":
                            WhisperSegmentCallback segCb = (WhisperSegmentCallback) args[4];
                            WhisperProgressCallback progCb = (WhisperProgressCallback) args[5];
                            if (segCb != null) {
                                segCb.onNewSegment(0, "Incremental text", 0L, 500L, null);
                            }
                            if (progCb != null) {
                                progCb.onProgress(50, null);
                            }
                            return 0;
                        case "whisper_full_n_segments":
                            return 1;
                        case "whisper_full_get_segment_t0":
                            return 0L;
                        case "whisper_full_get_segment_t1":
                            return 50L;
                        case "whisper_full_get_segment_text":
                            return "Incremental text";
                        case "whisper_full_n_tokens":
                            return 0;
                        case "whisper_bridge_detect_language":
                            return 0;
                        case "whisper_lang_str":
                            return "en";
                        case "whisper_free":
                            return null;
                        default:
                            return null;
                    }
                });

        AtomicBoolean segmentCalled = new AtomicBoolean(false);
        AtomicInteger lastProgress = new AtomicInteger(0);

        try (AndroidWhisperer w = new AndroidWhisperer(bridge)) {
            w.initModelFromFile(new File("dummy.bin"));
            w.addListener(new TranscriptionListener() {
                @Override
                public void onNewSegment(SegmentResult segment, String accumulatedTranscript) {
                    segmentCalled.set(true);
                    assertEquals("Incremental text", segment.getText());
                    assertEquals("Incremental text", accumulatedTranscript);
                }

                @Override
                public void onProgress(int progressPercent) {
                    lastProgress.set(progressPercent);
                }

                @Override
                public void onError(TranscriptionException exception) {
                    fail("Unexpected transcription error: " + exception.getMessage());
                }
            });

            TranscriptionResult result = w.transcribe(new float[]{0.1f});
            assertNotNull(result);
            assertTrue(segmentCalled.get());
            assertEquals(50, lastProgress.get());
        }
    }

    @Test
    public void transcribe_nativeFailure_throws() {
        WhisperBridge bridge = (WhisperBridge) Proxy.newProxyInstance(
                WhisperBridge.class.getClassLoader(),
                new Class<?>[]{WhisperBridge.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("whisper_init_from_file")) {
                        return new Pointer(999L);
                    } else if (name.equals("whisper_bridge_transcribe")) {
                        return -1;
                    }
                    return null;
                });

        try (AndroidWhisperer w = new AndroidWhisperer(bridge)) {
            w.initModelFromFile(new File("dummy.bin"));
            w.transcribe(new float[]{0.1f});
            fail("Expected TranscriptionException");
        } catch (TranscriptionException e) {
            assertEquals("Whisper inference failed with code -1", e.getMessage());
        }
    }

    @Test
    public void transcribe_nativeFailure_notifiesListenerOnError() {
        WhisperBridge bridge = (WhisperBridge) Proxy.newProxyInstance(
                WhisperBridge.class.getClassLoader(),
                new Class<?>[]{WhisperBridge.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("whisper_init_from_file")) {
                        return new Pointer(999L);
                    } else if (name.equals("whisper_bridge_transcribe_with_callback")) {
                        return -1;
                    }
                    return null;
                });

        AtomicBoolean errorCalled = new AtomicBoolean(false);

        try (AndroidWhisperer w = new AndroidWhisperer(bridge)) {
            w.initModelFromFile(new File("dummy.bin"));
            w.addListener(new TranscriptionListener() {
                @Override
                public void onNewSegment(SegmentResult segment, String accumulatedTranscript) {
                }

                @Override
                public void onProgress(int progressPercent) {
                }

                @Override
                public void onError(TranscriptionException exception) {
                    errorCalled.set(true);
                    assertEquals("Whisper inference failed with code -1", exception.getMessage());
                }
            });

            w.transcribe(new float[]{0.1f});
            fail("Expected TranscriptionException");
        } catch (TranscriptionException e) {
            assertEquals("Whisper inference failed with code -1", e.getMessage());
            assertTrue(errorCalled.get());
        }
    }

    @Test
    public void lifecycle_closedEngine_throws() {
        WhisperBridge bridge = (WhisperBridge) Proxy.newProxyInstance(
                WhisperBridge.class.getClassLoader(),
                new Class<?>[]{WhisperBridge.class},
                (proxy, method, args) -> null);

        AndroidWhisperer w = new AndroidWhisperer(bridge);
        w.close();
        w.release();

        try {
            w.initModelFromFile(new File("dummy.bin"));
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            assertEquals("Engine is closed", e.getMessage());
        }
    }
}
