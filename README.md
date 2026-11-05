# AndroidWhisperer

**AndroidWhisperer** is an Android library that provides a high-level, idiomatic Java API wrapping `whisper.cpp` for fast, local, on-device speech-to-text inference on Android.

## Goals & Features

- **Local & Offline Speech Recognition**: Run OpenAI's Whisper models locally on Android devices without server dependencies or cloud latency.
- **Ready-to-Use with Pre-Compiled Binaries**: Bundles pre-compiled native `whisper.cpp` engines optimized for **64-bit architectures** (`arm64-v8a` and `x86_64`). *Note: 32-bit architectures (`armeabi-v7a`, `x86`) are excluded as Whisper relies heavily on 64-bit ARM NEON/SIMD optimizations.* No native compilation or NDK is required for app developers.
- **Built-in Model Management**: Easily download, verify, and manage quantized GGML models ready-to-use via `ModelManager` and `WhisperModel`.
- **Language Auto-Detection**: Detect spoken languages directly from PCM sample arrays.
- **Timestamp & Pause Analysis**: Extract segment-level and token-level timestamps, as well as automatic pause/silence analysis between speech segments or tokens.
- **Audio Utilities**: Built-in WAV parser and PCM converter/resampler (`AudioUtils`) converting PCM16/float audio to 16 kHz mono float PCM required by Whisper.

---

## Installation & Setup

### Requirements

- **Android SDK API 24** (Android 7.0) or higher
- **Java 17** source/target compatibility
- **Android NDK**: *Only required if you are building the library or native C++ from source.* Regular app consumers do **not** need the NDK installed.

### Gradle Dependency

#### From Maven Central

In your `settings.gradle` (or root `build.gradle`), ensure Maven Central is declared:

```groovy
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
```

Then add the dependency to your app module's `build.gradle`:

```groovy
dependencies {
    implementation 'de.switch-consulting:androidwhisperer:0.1.0'
}
```

#### Project / Submodule Dependency

Alternatively, if including the project source directly, include the `:androidwhisperer` module in `settings.gradle`:

```groovy
include ':androidwhisperer'
```

And in your app module's `build.gradle`:

```groovy
dependencies {
    implementation project(':androidwhisperer')
}
```

---


## Predefined Models

The library includes built-in configurations via `WhisperModel` that point to quantized ggml model binaries hosted on Hugging Face:

| Constant | Variant | Approx. Disk Size | Languages | Recommended Purpose & Performance |
| :--- | :--- | :--- | :--- | :--- |
| `WhisperModel.TINY` | `tiny` | ~75 MB | Multilingual (99+ langs) | Fastest on-device inference, minimal RAM/disk footprint; ideal for lower-end devices or quick commands. |
| `WhisperModel.TINY_EN` | `tiny.en` | ~75 MB | English only | Optimized English model with speed matching `tiny` and higher English accuracy. |
| `WhisperModel.BASE` | `base` | ~142 MB | Multilingual (99+ langs) | **Recommended default**: Great trade-off between speed, memory usage, and transcription accuracy. |
| `WhisperModel.BASE_EN` | `base.en` | ~142 MB | English only | Better English accuracy than `tiny.en` while remaining very fast on modern smartphones. |
| `WhisperModel.SMALL` | `small` | ~465 MB | Multilingual (99+ langs) | Substantially higher accuracy for complex audio, accents, and diverse vocabularies; requires more RAM and CPU/GPU. |
| `WhisperModel.SMALL_EN` | `small.en` | ~465 MB | English only | High-accuracy transcription tailored strictly for English speech. |
| `WhisperModel.MEDIUM` | `medium` | ~1.5 GB | Multilingual (99+ langs) | High accuracy; recommended only for modern flagship devices with ample RAM and storage. |
| `WhisperModel.MEDIUM_EN` | `medium.en` | ~1.5 GB | English only | English-only counterpart of `medium`. |
| `WhisperModel.LARGE` | `large` | ~2.9 GB | Multilingual (99+ langs) | Maximum Whisper accuracy; very demanding on mobile resources. |

---

## Code Examples

### Java Example

#### 1. Initialize the Engine

```java
import de.switchconsulting.androidwhisperer.AndroidWhisperer;
import de.switchconsulting.androidwhisperer.ModelManager;
import de.switchconsulting.androidwhisperer.WhisperModel;

AndroidWhisperer whisperer = new AndroidWhisperer();

// Asynchronously download or ensure a model (e.g. WhisperModel.BASE or WhisperModel.TINY)
ModelManager.ensureModelAsync(context, WhisperModel.BASE, new ModelDownloadListener() {
    @Override
    public void onSuccess(File modelFile) {
        // Model file is downloaded and ready for use
        whisperer.init(context, WhisperModel.BASE);
    }

    @Override
    public void onProgress(long bytesDownloaded, long totalBytes) {
        // Download progress updates
    }

    @Override
    public void onError(Exception error) {
        // Handle error
    }
});
```

#### 2. Prepare Audio & Transcribe

```java
import de.switchconsulting.androidwhisperer.AudioUtils;
import de.switchconsulting.androidwhisperer.model.TranscriptionResult;
import de.switchconsulting.androidwhisperer.model.SegmentResult;
import de.switchconsulting.androidwhisperer.model.PauseSegment;
import java.io.File;

// Load a 16-bit WAV file or convert raw PCM16 bytes
float[] samples = AudioUtils.readWav(new File("/path/to/audio.wav"));

// Synchronous Transcribe
TranscriptionResult result = whisperer.transcribe(samples);
System.out.println("Language: " + result.getLanguage());
System.out.println("Text: " + result.getText());

// Asynchronous Transcribe (CompletableFuture)
whisperer.transcribeAsync(samples).thenAccept(asyncResult -> {
    System.out.println("Async Text: " + asyncResult.getText());
}).exceptionally(ex -> {
    ex.printStackTrace();
    return null;
});

// Access segment-level details & detected pauses
for (SegmentResult segment : result.getSegments()) {
    System.out.println(String.format("[%d ms - %d ms]: %s",
            segment.getStartMs(), segment.getEndMs(), segment.getText()));
}

// Language auto-detection
String lang = whisperer.detectLanguage(samples);

// Cleanup
whisperer.close();
```

---

### Kotlin Example

#### 1. Initialize the Engine

```kotlin
import de.switchconsulting.androidwhisperer.AndroidWhisperer
import de.switchconsulting.androidwhisperer.ModelDownloadListener
import de.switchconsulting.androidwhisperer.ModelManager
import de.switchconsulting.androidwhisperer.WhisperModel
import java.io.File

val whisperer = AndroidWhisperer()

ModelManager.ensureModelAsync(context, WhisperModel.BASE, object : ModelDownloadListener {
    override fun onSuccess(modelFile: File) {
        // Initialize the native engine once the model file is available
        whisperer.init(context, WhisperModel.BASE)
    }

    override fun onProgress(bytesDownloaded: Long, totalBytes: Long) {
        val progress = if (totalBytes > 0) (bytesDownloaded * 100 / totalBytes).toInt() else 0
        println("Download progress: $progress%")
    }

    override fun onError(error: Exception) {
        error.printStackTrace()
    }
})
```

#### 2. Prepare Audio & Transcribe

```kotlin
import de.switchconsulting.androidwhisperer.AudioUtils
import java.io.File

// Load WAV audio (automatically resampled & normalized to 16 kHz mono float)
val samples = AudioUtils.readWav(File("/path/to/audio.wav"))

// Or convert raw PCM bytes from an AudioRecord buffer:
// val samples = AudioUtils.fromPcm16(pcmBytes, sampleRate = 44100, channels = 2)

// Run transcription (best called on a background thread/coroutine dispatcher)
val result = whisperer.transcribe(samples)

println("Detected Language: ${result.language}")
println("Full Text: ${result.text}")

// Inspect segments with timestamps
result.segments.forEach { segment ->
    println("[${segment.startMs} ms - ${segment.endMs} ms]: ${segment.text}")
}

// Inspect silence/pause intervals
result.pauses.forEach { pause ->
    println("Pause of ${pause.durationMs} ms at ${pause.startMs} ms")
}

// Detect language standalone
val detectedLang = whisperer.detectLanguage(samples)
println("Detected language: $detectedLang")

// Always close the engine when done to free native memory
whisperer.close()
```

#### 3. Using with Coroutines (Idiomatic Kotlin)

```kotlin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Awaiting model download as a suspending function:
suspend fun ensureModel(context: Context, model: WhisperModel): File =
    suspendCancellableCoroutine { continuation ->
        ModelManager.ensureModelAsync(context, model, object : ModelDownloadListener {
            override fun onSuccess(modelFile: File) = continuation.resume(modelFile)
            override fun onProgress(bytesDownloaded: Long, totalBytes: Long) {}
            override fun onError(error: Exception) = continuation.resumeWithException(error)
        })
    }

// Offloading transcription to Dispatchers.Default / IO:
suspend fun transcribeAudio(whisperer: AndroidWhisperer, samples: FloatArray) =
    withContext(Dispatchers.Default) {
        whisperer.transcribe(samples)
    }
```

---

## Upstream Synchronization

This project includes `whisper.cpp` as a Git submodule located at `androidwhisperer/src/main/cpp/whisper.cpp`.

To initialize and update `whisper.cpp` to the latest upstream release:

```bash
git submodule update --init --remote androidwhisperer/src/main/cpp/whisper.cpp
```

Or checkout a specific tag:

```bash
cd androidwhisperer/src/main/cpp/whisper.cpp
git checkout v1.7.1
```

---

## License & Third-Party Notices

**AndroidWhisperer** is released under the [MIT License](LICENSE).

### Third-Party Libraries & Licenses

- **`whisper.cpp`**: MIT License (Copyright © The ggml authors). Included as a Git submodule at `androidwhisperer/src/main/cpp/whisper.cpp`.
- **OpenAI Whisper**: MIT License (Copyright © OpenAI).
- **Java Native Access (JNA)**: Apache License 2.0 / LGPL v2.1.
