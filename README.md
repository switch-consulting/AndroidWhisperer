# AndroidWhisperer

**AndroidWhisperer** is an Android library that provides a high-level, idiomatic Java API wrapping `whisper.cpp` for fast, local, on-device speech-to-text inference on Android.

## Goals & Features

- **Local & Offline Speech Recognition**: Run OpenAI's Whisper models locally on Android devices without server dependencies.
- **On-Device C++ Inference via CMake**: Bundles the native `whisper.cpp` C/C++ engine compiled directly for Android ABIs (`arm64-v8a`, `armeabi-v7a`, `x86_64`).
- **Language Auto-Detection**: Detect spoken languages directly from PCM sample arrays.
- **Timestamp & Pause Analysis**: Extract segment-level and token-level timestamps, as well as automatic pause/silence analysis between speech segments or tokens.
- **Audio Utilities**: Built-in WAV parser and PCM converter/resampler (`AudioUtils`) converting PCM16/float audio to 16 kHz mono float PCM required by Whisper.

---

## Installation & Setup

### Requirements

- Android SDK API 23 (Android 6.0) or higher
- Android NDK (installed via Android Studio SDK Manager)
- Java 17

### Gradle Dependency

#### 1. From Maven Central (Releases & Snapshots)

In your `settings.gradle` (or root `build.gradle`), ensure Maven Central and the Sonatype Snapshot repository are declared:

```groovy
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
        }
    }
}
```

Then add the dependency to your app module's `build.gradle`:

```groovy
dependencies {
    // Release version:
    implementation 'de.switch-consulting:androidwhisperer:0.1.0'

    // Or latest Snapshot:
    // implementation 'de.switch-consulting:androidwhisperer:0.1.0-SNAPSHOT'
}
```

#### 2. Project / Submodule Dependency

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

## Publishing

The project uses the Vanniktech Gradle Maven Publish Plugin with Sonatype Central Portal and PGP signing.

### Prerequisites

Define your credentials in `~/.gradle/gradle.properties` (or `C:\Users\<user>\.gradle\gradle.properties`):

```properties
# Sonatype Central Portal User Token
mavenCentralUsername=YOUR_TOKEN_USERNAME
mavenCentralPassword=YOUR_TOKEN_PASSWORD

# PGP Signing Key & Passphrase
signing.key=-----BEGIN PGP PRIVATE KEY BLOCK-----\n...\n-----END PGP PRIVATE KEY BLOCK-----
signing.password=YOUR_PGP_PASSPHRASE
```

### Publishing Commands

| Destination | Command / Task | Description |
| :--- | :--- | :--- |
| **Local Maven repository** (`~/.m2`) | `.\gradlew :androidwhisperer:publishToMavenLocal` | Builds and installs the library to your local Maven cache. |
| **Maven Central Snapshots** | `.\gradlew :androidwhisperer:publish` | When `VERSION_NAME` in `gradle.properties` ends with `-SNAPSHOT`, publishes signed artifacts to `https://central.sonatype.com/repository/maven-snapshots/`. |
| **Maven Central Releases** | `.\gradlew :androidwhisperer:publish` | When `VERSION_NAME` is a release (e.g. `0.1.0`), creates a portal bundle and uploads to Maven Central Portal with automatic release. |

> [!TIP]
> In Android Studio, these tasks can also be run from the **Gradle** tool window under `:androidwhisperer` ➔ `Tasks` ➔ `publishing`.

---

## Usage

### 0. Add Dependency (Releases & Snapshots)

In your `settings.gradle`:

```groovy
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        // Required for SNAPSHOT builds:
        maven {
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
        }
    }
}
```

In your app's `build.gradle`:

```groovy
dependencies {
    // Current snapshot:
    implementation 'de.switch-consulting:androidwhisperer:0.1.0-SNAPSHOT'

    // Or release (when published):
    // implementation 'de.switch-consulting:androidwhisperer:0.1.0'
}
```

### 1. Initialize the Engine

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

### 2. Prepare Audio Samples

Whisper requires mono PCM float samples normalized to `[-1.0, 1.0]` at 16,000 Hz. Use `AudioUtils` to convert WAV files or PCM buffers:

```java
import de.switchconsulting.androidwhisperer.AudioUtils;
import java.io.File;

// Load a 16-bit WAV file directly
float[] samples = AudioUtils.readWav(new File("/path/to/audio.wav"));

// Or convert raw PCM16 bytes (e.g., from AudioRecord at 44.1 kHz stereo)
byte[] pcmBytes = ...;
float[] normalizedSamples = AudioUtils.fromPcm16(pcmBytes, 44100, 2);
```

### 3. Transcribe Audio

```java
import de.switchconsulting.androidwhisperer.model.TranscriptionResult;
import de.switchconsulting.androidwhisperer.model.SegmentResult;
import de.switchconsulting.androidwhisperer.model.PauseSegment;

// Execute transcription
TranscriptionResult result = whisperer.transcribe(samples);

System.out.println("Language: " + result.getLanguage());
System.out.println("Text: " + result.getText());

// Access segment-level details
for (SegmentResult segment : result.getSegments()) {
    System.out.println(String.format("[%d ms - %d ms]: %s",
            segment.getStartMs(), segment.getEndMs(), segment.getText()));
}

// Access detected pause/silence intervals
for (PauseSegment pause : result.getPauses()) {
    System.out.println(String.format("Pause of %d ms at %d ms",
            pause.getDurationMs(), pause.getStartMs()));
}
```

### 4. Language Auto-Detection

```java
String detectedLanguage = whisperer.detectLanguage(samples);
System.out.println("Detected language: " + detectedLanguage); // e.g., "en", "de", "fr"
```

### 5. Cleanup Resources

```java
// Close the engine when done to release native context memory
whisperer.close();
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
