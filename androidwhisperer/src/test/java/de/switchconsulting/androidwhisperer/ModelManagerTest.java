package de.switchconsulting.androidwhisperer;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ModelManagerTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void whisperModel_defaultDownloadUrl_formatsVariantNameCorrectly() {
        WhisperModel tinyModel = new WhisperModel("tiny", "ggml-tiny.bin", null);
        assertEquals(
                "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin",
                tinyModel.getDownloadUrl());

        WhisperModel tinyEnModel = new WhisperModel("tiny.en", "ggml-tiny.en.bin", null);
        assertEquals(
                "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.en.bin",
                tinyEnModel.getDownloadUrl());

        WhisperModel baseModel = new WhisperModel("ggml-base", "ggml-base.bin", null);
        assertEquals(
                "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin",
                baseModel.getDownloadUrl());

        WhisperModel smallModel = new WhisperModel("small.bin", "ggml-small.bin", null);
        assertEquals(
                "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small.bin",
                smallModel.getDownloadUrl());
    }

    @Test(expected = IllegalArgumentException.class)
    public void whisperModel_throwsOnNullVariant() {
        new WhisperModel(null, "file.bin", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void whisperModel_throwsOnEmptyVariant() {
        new WhisperModel("   ", "file.bin", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void whisperModel_throwsOnNullFileName() {
        new WhisperModel("variant", null, null);
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
        assertEquals("https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin", WhisperModel.TINY.getDownloadUrl());
        assertTrue(WhisperModel.TINY.isAsset());

        assertEquals("base", WhisperModel.BASE.getVariant());
        assertEquals("ggml-base.bin", WhisperModel.BASE.getFileName());
        assertNull(WhisperModel.BASE.getAssetPath());
        assertEquals("https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin", WhisperModel.BASE.getDownloadUrl());
        assertFalse(WhisperModel.BASE.isAsset());
    }

    @Test
    public void customWhisperModel_instantiation() {
        WhisperModel customModel = new WhisperModel("custom", "ggml-custom.bin", null, "https://example.com/ggml-custom.bin");

        assertEquals("custom", customModel.getVariant());
        assertEquals("ggml-custom.bin", customModel.getFileName());
        assertNull(customModel.getAssetPath());
        assertEquals("https://example.com/ggml-custom.bin", customModel.getDownloadUrl());
        assertFalse(customModel.isAsset());
        assertNotNull(customModel.getLocalFile(new android.content.ContextWrapper(null) {
            @Override
            public File getFilesDir() {
                return temporaryFolder.getRoot();
            }
        }));
    }

    @Test
    public void customWhisperModel_withoutExplicitUrl_usesCalculatedDefault() {
        WhisperModel customModel = new WhisperModel("custom-variant", "ggml-custom.bin", null, null);
        assertEquals(
                "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-custom-variant.bin",
                customModel.getDownloadUrl());
    }

    @Test(expected = IOException.class)
    public void modelManager_ensureModel_throwsIOExceptionOnFailedDownload() throws Exception {
        android.content.Context mockContext = new android.content.ContextWrapper(null) {
            @Override
            public File getFilesDir() {
                return temporaryFolder.getRoot();
            }
        };
        WhisperModel nonExistentModel = new WhisperModel("nonexistent-variant", "ggml-nonexistent.bin", null);
        ModelManager.ensureModel(mockContext, nonExistentModel);
    }

    @Test
    public void modelManager_ensureModel_returnsExistingLocalFile() throws Exception {
        android.content.Context mockContext = new android.content.ContextWrapper(null) {
            @Override
            public File getFilesDir() {
                return temporaryFolder.getRoot();
            }
        };

        File modelsDir = ModelManager.getModelsDir(mockContext);
        File localModel = new File(modelsDir, "ggml-local.bin");
        try (FileOutputStream out = new FileOutputStream(localModel)) {
            out.write("fake model data".getBytes());
        }

        WhisperModel model = new WhisperModel("local", "ggml-local.bin", null, "https://example.com/unused");
        assertTrue(ModelManager.isModelAvailable(mockContext, model));

        File result = ModelManager.ensureModel(mockContext, model);
        assertEquals(localModel.getAbsolutePath(), result.getAbsolutePath());
    }

    @Test
    public void modelManager_isModelAvailable_returnsFalseForNullContextOrModel() {
        assertFalse(ModelManager.isModelAvailable(null, WhisperModel.BASE));
        assertFalse(ModelManager.isModelAvailable(null, null));
        android.content.Context mockContext = new android.content.ContextWrapper(null) {
            @Override
            public File getFilesDir() {
                return temporaryFolder.getRoot();
            }
        };
        assertFalse(ModelManager.isModelAvailable(mockContext, null));
    }

    @Test
    public void modelManager_isModelAvailable_returnsTrueForModelWithDownloadUrl() {
        android.content.Context mockContext = new android.content.ContextWrapper(null) {
            @Override
            public File getFilesDir() {
                return temporaryFolder.getRoot();
            }
        };
        assertTrue(ModelManager.isModelAvailable(mockContext, WhisperModel.BASE));
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

    @Test(expected = IOException.class)
    public void downloadModelFromUrl_throwsOnInvalidHost() throws Exception {
        File targetFile = new File(temporaryFolder.getRoot(), "invalid.bin");
        ModelManager.downloadModelFromUrl("http://invalid.domain.that.does.not.exist.local/model.bin", targetFile);
    }

    @Test
    public void ensureModelAsync_callsOnSuccessForExistingLocalFile() throws Exception {
        android.content.Context mockContext = new android.content.ContextWrapper(null) {
            @Override
            public File getFilesDir() {
                return temporaryFolder.getRoot();
            }
        };

        File modelsDir = ModelManager.getModelsDir(mockContext);
        File localModel = new File(modelsDir, "ggml-local-async.bin");
        try (FileOutputStream out = new FileOutputStream(localModel)) {
            out.write("fake model data".getBytes());
        }

        WhisperModel model = new WhisperModel("local-async", "ggml-local-async.bin", null, "https://example.com/unused");
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<File> resultFile = new java.util.concurrent.atomic.AtomicReference<>();

        ModelManager.ensureModelAsync(mockContext, model, new ModelDownloadListener() {
            @Override
            public void onSuccess(File modelFile) {
                resultFile.set(modelFile);
                latch.countDown();
            }

            @Override
            public void onError(Exception error) {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, java.util.concurrent.TimeUnit.SECONDS));
        assertNotNull(resultFile.get());
        assertEquals(localModel.getAbsolutePath(), resultFile.get().getAbsolutePath());
    }

    @Test
    public void ensureModelAsync_callsOnErrorOnDownloadFailure() throws Exception {
        android.content.Context mockContext = new android.content.ContextWrapper(null) {
            @Override
            public File getFilesDir() {
                return temporaryFolder.getRoot();
            }
        };

        WhisperModel nonExistentModel = new WhisperModel("invalid-host-model", "ggml-invalid.bin", null, "http://invalid.domain.that.does.not.exist.local/model.bin");
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Exception> resultError = new java.util.concurrent.atomic.AtomicReference<>();

        ModelManager.ensureModelAsync(mockContext, nonExistentModel, new ModelDownloadListener() {
            @Override
            public void onSuccess(File modelFile) {
                latch.countDown();
            }

            @Override
            public void onError(Exception error) {
                resultError.set(error);
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, java.util.concurrent.TimeUnit.SECONDS));
        assertNotNull(resultError.get());
    }

    @Test
    public void downloadModelAsync_callsOnErrorOnFailure() throws Exception {
        android.content.Context mockContext = new android.content.ContextWrapper(null) {
            @Override
            public File getFilesDir() {
                return temporaryFolder.getRoot();
            }
        };

        WhisperModel invalidModel = new WhisperModel("invalid-host", "ggml-invalid2.bin", null, "http://invalid.domain.that.does.not.exist.local/model.bin");
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Exception> resultError = new java.util.concurrent.atomic.AtomicReference<>();

        ModelManager.downloadModelAsync(mockContext, invalidModel, new ModelDownloadListener() {
            @Override
            public void onSuccess(File modelFile) {
                latch.countDown();
            }

            @Override
            public void onError(Exception error) {
                resultError.set(error);
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, java.util.concurrent.TimeUnit.SECONDS));
        assertNotNull(resultError.get());
    }
}
