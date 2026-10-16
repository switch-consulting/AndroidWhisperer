package de.switchconsulting.androidwhisperer;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ModelManagerTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void getDownloadUrl_formatsVariantNameCorrectly() {
        assertEquals(
                "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin",
                ModelManager.getDownloadUrl("tiny"));

        assertEquals(
                "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.en.bin",
                ModelManager.getDownloadUrl("tiny.en"));

        assertEquals(
                "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin",
                ModelManager.getDownloadUrl("ggml-base"));

        assertEquals(
                "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small.bin",
                ModelManager.getDownloadUrl("small.bin"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getDownloadUrl_throwsOnNull() {
        ModelManager.getDownloadUrl(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getDownloadUrl_throwsOnEmpty() {
        ModelManager.getDownloadUrl("   ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void getModelsDir_throwsOnNullContext() {
        ModelManager.getModelsDir(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureModelFromAssets_throwsOnNullContext() throws Exception {
        ModelManager.ensureModelFromAssets(null, "models/ggml-tiny.bin");
    }

    @Test(expected = IllegalArgumentException.class)
    public void copyAssetToFile_throwsOnNullTargetFile() throws Exception {
        ModelManager.copyAssetToFile(null, "path", null);
    }

    @Test
    public void whisperModel_properties() {
        assertEquals("tiny", WhisperModel.TINY.getVariant());
        assertEquals("ggml-tiny.bin", WhisperModel.TINY.getFileName());
        assertEquals("models/ggml-tiny.bin", WhisperModel.TINY.getAssetPath());
        assertTrue(WhisperModel.TINY.isSupported());
        assertFalse(WhisperModel.BASE.isSupported());
    }

    @Test(expected = WhisperModelNotAvailable.class)
    public void modelManager_ensureModel_throwsOnUnsupportedModel() throws Exception {
        ModelManager.ensureModel(null, WhisperModel.BASE);
    }

    @Test
    public void modelManager_isModelAvailable_returnsFalseForUnsupported() {
        assertFalse(ModelManager.isModelAvailable(null, WhisperModel.BASE));
        assertFalse(ModelManager.isModelAvailable(null, null));
    }

    @Test(expected = WhisperModelNotAvailable.class)
    public void androidWhisperer_init_throwsOnUnsupportedModel() throws Exception {
        de.switchconsulting.androidwhisperer.internal.WhisperBridge dummyBridge =
                (de.switchconsulting.androidwhisperer.internal.WhisperBridge)
                        java.lang.reflect.Proxy.newProxyInstance(
                                de.switchconsulting.androidwhisperer.internal.WhisperBridge.class.getClassLoader(),
                                new Class<?>[]{de.switchconsulting.androidwhisperer.internal.WhisperBridge.class},
                                (proxy, method, args) -> null);

        try (AndroidWhisperer whisperer = new AndroidWhisperer(dummyBridge)) {
            whisperer.init(null, WhisperModel.BASE);
        }
    }

    @Test
    public void initModelFromFile_throwsOnNullFile() {
        de.switchconsulting.androidwhisperer.internal.WhisperBridge dummyBridge =
                (de.switchconsulting.androidwhisperer.internal.WhisperBridge)
                        java.lang.reflect.Proxy.newProxyInstance(
                                de.switchconsulting.androidwhisperer.internal.WhisperBridge.class.getClassLoader(),
                                new Class<?>[]{de.switchconsulting.androidwhisperer.internal.WhisperBridge.class},
                                (proxy, method, args) -> null);

        try (AndroidWhisperer whisperer = new AndroidWhisperer(dummyBridge)) {
            whisperer.initModelFromFile(null);
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("modelFile == null", e.getMessage());
        }
    }
}
