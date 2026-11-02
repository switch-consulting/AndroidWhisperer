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
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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
                            WhisperSegmentCallback segCb = (WhisperSegmentCallback) args[5];
                            WhisperProgressCallback progCb = (WhisperProgressCallback) args[6];
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
    public void transcribe_preselectedLanguage_multilingualModel_success() {
        AtomicReference<String> passedLanguage = new AtomicReference<>();
        WhisperBridge bridge = (WhisperBridge) Proxy.newProxyInstance(
                WhisperBridge.class.getClassLoader(),
                new Class<?>[]{WhisperBridge.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    switch (name) {
                        case "whisper_init_from_file":
                            return new Pointer(999L);
                        case "whisper_is_multilingual":
                            return 1;
                        case "whisper_lang_id":
                            return "de".equals(args[0]) ? 2 : -1;
                        case "whisper_bridge_transcribe":
                            passedLanguage.set((String) args[4]);
                            return 0;
                        case "whisper_full_n_segments":
                            return 1;
                        case "whisper_full_get_segment_t0":
                            return 0L;
                        case "whisper_full_get_segment_t1":
                            return 100L;
                        case "whisper_full_get_segment_text":
                            return "Hallo Welt";
                        case "whisper_full_n_tokens":
                            return 0;
                        case "whisper_free":
                            return null;
                        default:
                            return null;
                    }
                });

        try (AndroidWhisperer w = new AndroidWhisperer(bridge)) {
            w.initModelFromFile(new File("dummy.bin"));
            AndroidWhispererOptions options = new AndroidWhispererOptions(
                    PauseMode.BOTH, 300, 100, Locale.GERMAN);
            TranscriptionResult result = w.transcribe(new float[]{0.1f}, options);
            assertNotNull(result);
            assertEquals("Hallo Welt", result.getText());
            assertEquals("de", result.getLanguage());
            assertEquals("de", passedLanguage.get());
        }
    }

    @Test
    public void transcribe_preselectedLanguage_englishOnlyModel_englishAllowed() {
        AtomicReference<String> passedLanguage = new AtomicReference<>();
        WhisperBridge bridge = (WhisperBridge) Proxy.newProxyInstance(
                WhisperBridge.class.getClassLoader(),
                new Class<?>[]{WhisperBridge.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    switch (name) {
                        case "whisper_init_from_file":
                            return new Pointer(999L);
                        case "whisper_is_multilingual":
                            return 0;
                        case "whisper_bridge_transcribe":
                            passedLanguage.set((String) args[4]);
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
                            return 0;
                        case "whisper_free":
                            return null;
                        default:
                            return null;
                    }
                });

        try (AndroidWhisperer w = new AndroidWhisperer(bridge)) {
            w.initModelFromFile(new File("dummy.bin"));
            AndroidWhispererOptions options = new AndroidWhispererOptions(
                    PauseMode.BOTH, 300, 100, Locale.ENGLISH);
            TranscriptionResult result = w.transcribe(new float[]{0.1f}, options);
            assertNotNull(result);
            assertEquals("en", result.getLanguage());
            assertEquals("en", passedLanguage.get());
        }
    }

    @Test
    public void transcribe_preselectedLanguage_englishOnlyModel_nonEnglishThrowsException() {
        WhisperBridge bridge = (WhisperBridge) Proxy.newProxyInstance(
                WhisperBridge.class.getClassLoader(),
                new Class<?>[]{WhisperBridge.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("whisper_init_from_file".equals(name)) {
                        return new Pointer(999L);
                    } else if ("whisper_is_multilingual".equals(name)) {
                        return 0; // English only
                    }
                    return null;
                });

        try (AndroidWhisperer w = new AndroidWhisperer(bridge)) {
            w.initModelFromFile(new File("dummy.bin"));
            AndroidWhispererOptions options = new AndroidWhispererOptions(
                    PauseMode.BOTH, 300, 100, Locale.GERMAN);
            w.transcribe(new float[]{0.1f}, options);
            fail("Expected LanguageNotSupportedException");
        } catch (LanguageNotSupportedException e) {
            assertEquals(Locale.GERMAN, e.getLocale());
            assertTrue(e.getMessage().contains("Language not supported"));
        }
    }

    @Test
    public void transcribe_preselectedLanguage_multilingualModel_unsupportedLanguageThrowsException() {
        WhisperBridge bridge = (WhisperBridge) Proxy.newProxyInstance(
                WhisperBridge.class.getClassLoader(),
                new Class<?>[]{WhisperBridge.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("whisper_init_from_file".equals(name)) {
                        return new Pointer(999L);
                    } else if ("whisper_is_multilingual".equals(name)) {
                        return 1;
                    } else if ("whisper_lang_id".equals(name)) {
                        return -1; // Unsupported
                    }
                    return null;
                });

        Locale unsupportedLocale = new Locale("xyz");
        try (AndroidWhisperer w = new AndroidWhisperer(bridge)) {
            w.initModelFromFile(new File("dummy.bin"));
            AndroidWhispererOptions options = new AndroidWhispererOptions(
                    PauseMode.BOTH, 300, 100, unsupportedLocale);
            w.transcribe(new float[]{0.1f}, options);
            fail("Expected LanguageNotSupportedException");
        } catch (LanguageNotSupportedException e) {
            assertEquals(unsupportedLocale, e.getLocale());
            assertTrue(e.getMessage().contains("Language not supported"));
        }
    }

    @Test
    public void transcribe_preselectedLanguage_emptyLanguageCode_throwsException() {
        WhisperBridge bridge = (WhisperBridge) Proxy.newProxyInstance(
                WhisperBridge.class.getClassLoader(),
                new Class<?>[]{WhisperBridge.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("whisper_init_from_file".equals(name)) {
                        return new Pointer(999L);
                    } else if ("whisper_is_multilingual".equals(name)) {
                        return 1;
                    }
                    return null;
                });

        Locale emptyLocale = new Locale("");
        try (AndroidWhisperer w = new AndroidWhisperer(bridge)) {
            w.initModelFromFile(new File("dummy.bin"));
            AndroidWhispererOptions options = new AndroidWhispererOptions(
                    PauseMode.BOTH, 300, 100, emptyLocale);
            w.transcribe(new float[]{0.1f}, options);
            fail("Expected LanguageNotSupportedException");
        } catch (LanguageNotSupportedException e) {
            assertEquals(emptyLocale, e.getLocale());
            assertTrue(e.getMessage().contains("Language not supported"));
        }
    }

    @Test
    public void transcribe_preselectedLanguage_notifiesListenerOnError() {
        WhisperBridge bridge = (WhisperBridge) Proxy.newProxyInstance(
                WhisperBridge.class.getClassLoader(),
                new Class<?>[]{WhisperBridge.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("whisper_init_from_file".equals(name)) {
                        return new Pointer(999L);
                    } else if ("whisper_is_multilingual".equals(name)) {
                        return 0; // English only
                    }
                    return null;
                });

        AtomicBoolean listenerNotified = new AtomicBoolean(false);

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
                    listenerNotified.set(true);
                    assertTrue(exception instanceof LanguageNotSupportedException);
                    assertEquals(Locale.GERMAN, ((LanguageNotSupportedException) exception).getLocale());
                }
            });

            AndroidWhispererOptions options = new AndroidWhispererOptions(
                    PauseMode.BOTH, 300, 100, Locale.GERMAN);
            w.transcribe(new float[]{0.1f}, options);
            fail("Expected LanguageNotSupportedException");
        } catch (LanguageNotSupportedException e) {
            assertTrue(listenerNotified.get());
        }
    }

    @Test
    public void transcribe_preselectedLanguage_usesLoadedWhisperModelDirectly() {
        WhisperBridge bridge = (WhisperBridge) Proxy.newProxyInstance(
                WhisperBridge.class.getClassLoader(),
                new Class<?>[]{WhisperBridge.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("whisper_init_from_file".equals(name)) {
                        return new Pointer(999L);
                    } else if ("whisper_is_multilingual".equals(name)) {
                        return 1;
                    } else if ("whisper_bridge_transcribe".equals(name)) {
                        return 0;
                    } else if ("whisper_full_n_segments".equals(name)) {
                        return 0;
                    } else if ("whisper_free".equals(name)) {
                        return null;
                    }
                    return null;
                });

        try (AndroidWhisperer w = new AndroidWhisperer(bridge)) {
            w.initModelFromFile(new File("ggml-tiny.bin"));
            // German is supported by multilingual model
            AndroidWhispererOptions germanOpts = new AndroidWhispererOptions(
                    PauseMode.BOTH, 300, 100, Locale.GERMAN);
            assertNotNull(w.transcribe(new float[]{0.1f}, germanOpts));
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
