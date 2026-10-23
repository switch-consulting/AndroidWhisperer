package de.switchconsulting.androidwhisperer;

import com.sun.jna.Pointer;

import org.junit.Test;

import de.switchconsulting.androidwhisperer.model.TranscriptionResult;

import java.io.File;
import java.lang.reflect.Proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class AndroidWhispererTest {

    @Test
    public void options_validAndDefaults() {
        AndroidWhispererOptions opts = AndroidWhispererOptions.defaults();
        assertNotNull(opts);

        AndroidWhispererOptions custom = new AndroidWhispererOptions(
                AndroidWhisperer.PauseMode.TOKEN, 500, 150);
        assertNotNull(custom);
    }

    @Test(expected = IllegalArgumentException.class)
    public void options_nullPauseMode_throws() {
        new AndroidWhispererOptions(null, 300, 100);
    }

    @Test(expected = IllegalArgumentException.class)
    public void options_negativeSegmentThreshold_throws() {
        new AndroidWhispererOptions(AndroidWhisperer.PauseMode.BOTH, -1, 100);
    }

    @Test(expected = IllegalArgumentException.class)
    public void options_negativeTokenThreshold_throws() {
        new AndroidWhispererOptions(AndroidWhisperer.PauseMode.BOTH, 300, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_nullBridge_throws() {
        new AndroidWhisperer(null);
    }

    @Test
    public void detectLanguage_success() {
        de.switchconsulting.androidwhisperer.internal.WhisperBridge initBridge =
                (de.switchconsulting.androidwhisperer.internal.WhisperBridge)
                        Proxy.newProxyInstance(
                                de.switchconsulting.androidwhisperer.internal.WhisperBridge.class.getClassLoader(),
                                new Class<?>[]{de.switchconsulting.androidwhisperer.internal.WhisperBridge.class},
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
        de.switchconsulting.androidwhisperer.internal.WhisperBridge bridge =
                (de.switchconsulting.androidwhisperer.internal.WhisperBridge)
                        Proxy.newProxyInstance(
                                de.switchconsulting.androidwhisperer.internal.WhisperBridge.class.getClassLoader(),
                                new Class<?>[]{de.switchconsulting.androidwhisperer.internal.WhisperBridge.class},
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
    public void transcribe_nativeFailure_throws() {
        de.switchconsulting.androidwhisperer.internal.WhisperBridge bridge =
                (de.switchconsulting.androidwhisperer.internal.WhisperBridge)
                        Proxy.newProxyInstance(
                                de.switchconsulting.androidwhisperer.internal.WhisperBridge.class.getClassLoader(),
                                new Class<?>[]{de.switchconsulting.androidwhisperer.internal.WhisperBridge.class},
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
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            assertEquals("Whisper inference failed with code -1", e.getMessage());
        }
    }

    @Test
    public void lifecycle_closedEngine_throws() {
        de.switchconsulting.androidwhisperer.internal.WhisperBridge bridge =
                (de.switchconsulting.androidwhisperer.internal.WhisperBridge)
                        Proxy.newProxyInstance(
                                de.switchconsulting.androidwhisperer.internal.WhisperBridge.class.getClassLoader(),
                                new Class<?>[]{de.switchconsulting.androidwhisperer.internal.WhisperBridge.class},
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
