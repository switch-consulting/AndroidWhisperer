# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.2.0-SNAPSHOT]

### Added
- Added `SamplingStrategy` enum (`GREEDY`, `BEAM_SEARCH`) to configure Whisper decoding strategy.
- Added support for CPU thread count (`withThreads(int)`), sampling strategy (`withStrategy(SamplingStrategy)`), and translation (`withTranslate(boolean)`) in `AndroidWhispererOptions` and native C++ bridge.
- Added `translatedLanguage` field (`Locale`) to `TranscriptionResult` to report the target language when translation is enabled.
- Added `TranscriptionItem` interface and `TokenResult` model to support unified chronological timeline items (speech segments, tokens, and pause intervals).
- Added `toJSON()` serialization methods to `PauseSegment`, `SegmentResult`, `TokenResult`, and `TranscriptionResult`.
- Added default pause mode, threshold, thread count, and sampling strategy constants to `AndroidWhispererOptions` and `PauseMode`.
- Added `@author` Javadoc tags across library and demo source files.

### Changed
- Changed `language` property type in `TranscriptionResult` from `String` to `Locale`.
- Unified transcription handling with `TranscriptionItem` interface in `AndroidWhisperer`.
- Updated demo app (`MainActivity`) to render a chronological timeline of `TranscriptionItem`s, tokens, and pauses.
- Updated build source sets and Gradle dependencies.

### Removed
- Removed legacy constructors and `getAllPauses()` method from `TranscriptionResult` in favor of unified `getItems()` or filtering items.

## [0.1.0] - Initial Release

### Added
- Initial public release of **AndroidWhisperer** (`de.switch-consulting:androidwhisperer:0.1.0`) on Maven Central.
- High-level, idiomatic Java and Kotlin API wrapping `whisper.cpp` for fast, local, on-device speech-to-text inference on Android.
- Pre-compiled native `whisper.cpp` engines optimized for 64-bit architectures (`arm64-v8a` and `x86_64`). No NDK or native compilation required for app developers.
- `ModelManager` for asynchronous model downloading, verification, and management of quantized GGML models (`tiny`, `base`, `small`, `medium`, `large`, and English-only variants).
- Segment-level and token-level timestamps, plus automatic silence/pause analysis (`SegmentResult`, `PauseSegment`).
- Language auto-detection directly from PCM sample arrays.
- `AudioUtils` WAV parser and PCM converter/resampler (converting PCM16/float audio to 16 kHz mono float PCM).
- Full Kotlin Coroutines and Java `CompletableFuture` support.
