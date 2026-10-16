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

Include the `:androidwhisperer` module in your project's `settings.gradle`:

```groovy
include ':androidwhisperer'
```

And add the dependency to your app module's `build.gradle`:

```groovy
dependencies {
    implementation project(':androidwhisperer')
}
```

---

## Usage

### 1. Initialize the Engine

```java
import de.switchconsulting.androidwhisperer.AndroidWhisperer;
import de.switchconsulting.androidwhisperer.WhisperModel;

AndroidWhisperer whisperer = new AndroidWhisperer();

// Initialize with the default model (WhisperModel.TINY) extracted automatically from assets
whisperer.init(context);

// Or check availability and initialize with a specific model enum
if (AndroidWhisperer.isModelAvailable(context, WhisperModel.TINY)) {
    whisperer.init(context, WhisperModel.TINY);
}
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

To update `whisper.cpp` to the latest upstream release:

```bash
git submodule update --remote androidwhisperer/src/main/cpp/whisper.cpp
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
