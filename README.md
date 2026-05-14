# Audx Android

[![](https://jitpack.io/v/rizukirr/audx-android.svg)](https://jitpack.io/#rizukirr/audx-android)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

Blazingly Fast Real-time audio denoising and Voice Activity Detection (VAD) library for Android

> [NOTE] v2.1.0 or latest use gradle 9.0^ which is breaking changes, use v2.0.0 if you are not yet migrate to gradle 9.0

## Features

- **Real-time noise suppression** using Recurrent Neural Networks (RNNoise)
- **Voice Activity Detection (VAD)** with probability scores (0.0-1.0)
- **Automatic sample rate conversion** - works with any input rate (8kHz, 16kHz, 48kHz, etc.)
- **High performance** - SIMD optimized for ARM64, ARMv7 (NEON required), and x86_64
- **Coroutine support** - async processing with Kotlin coroutines
- **Small footprint** - native libraries optimized for mobile
- **Simple API** - easy integration with AudioRecord/AudioTrack

## Installation

Add JitPack repository to your `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io") // Add this
    }
}
```

Add the dependency to your app's `build.gradle.kts`. From `v2.1.2` the library is split into per-ABI modules so consumers can either pull the slim default or cherry-pick exactly the ABIs they ship.

**Slim default** (arm64-v8a + x86_64 — covers production phones and most emulators):

```kotlin
dependencies {
    implementation("com.github.rizukirr.audx-android:audx:v2.1.2")
}
```

**Cherry-pick** (e.g. arm64-only, or armv7 device support):

```kotlin
dependencies {
    implementation("com.github.rizukirr.audx-android:audx-core:v2.1.2")

    // Pick any subset:
    implementation("com.github.rizukirr.audx-android:audx-arm64-v8a:v2.1.2")
    implementation("com.github.rizukirr.audx-android:audx-armeabi-v7a:v2.1.2") // armv7
    implementation("com.github.rizukirr.audx-android:audx-x86_64:v2.1.2")       // emulator on Intel hosts
}
```

Each per-ABI module ships only that ABI's native libraries (~15 MB), so cherry-picking keeps your APK lean. Replace the version with the latest tag from the JitPack badge above.

> **Optional but recommended for true single-ABI APKs:** the cherry-pick above controls *audx*'s footprint, but transitive deps from AndroidX/Compose can still ship universal native libs (covering all four ABIs). To strip those too, add an `abiFilters` clamp in your app module's `defaultConfig`:
>
> ```kotlin
> android {
>     defaultConfig {
>         ndk {
>             abiFilters.addAll(setOf("arm64-v8a"))  // or your target set
>         }
>     }
> }
> ```
>
> `abiFilters` and the cherry-pick solve different problems and compose cleanly — use both together when you want a truly arm64-only (or armv7-only, etc.) APK.

## Quick Start

### Basic Usage

```kotlin
import com.audx.android.Audx

class VoiceRecorder {
    private val audx = Audx.Builder()
        .inputRate(16000)  // Match your AudioRecord sample rate
        .resampleQuality(Audx.AUDX_RESAMPLER_QUALITY_VOIP)
        .build()

    fun processAudioChunk(audioData: ShortArray) {
        val outputBuffer = ShortArray(audioData.size)

        audx.process(audioData, outputBuffer) { vadProbability ->
            if (vadProbability > 0.5f) {
                println("Voice detected! Probability: $vadProbability")
            } else {
                println("Silence or noise")
            }
        }

        // outputBuffer now contains denoised audio
        playAudio(outputBuffer)
    }

    fun cleanup() {
        audx.close()
    }
}
```

### Real-time Microphone Processing

```kotlin
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.audx.android.Audx

class AudioViewModel : ViewModel() {
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    private val audx = Audx.Builder()
        .inputRate(sampleRate)
        .resampleQuality(Audx.AUDX_RESAMPLER_QUALITY_VOIP)
        .build()

    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    fun startRecording() {
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        audioRecord?.startRecording()
        isRecording = true

        viewModelScope.launch(Dispatchers.IO) {
            val buffer = ShortArray(bufferSize)
            val outputBuffer = ShortArray(bufferSize)

            while (isRecording) {
                val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0

                if (readSize > 0) {
                    // Process audio with denoising
                    audx.process(buffer, outputBuffer) { vadProbability ->
                        if (vadProbability > 0.5f) {
                            // Voice detected - could trigger recording, transcription, etc.
                            onVoiceDetected(vadProbability)
                        }
                    }

                    // Use outputBuffer for playback, streaming, or storage
                    processCleanAudio(outputBuffer, readSize)
                }
            }
        }
    }

    fun stopRecording() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    override fun onCleared() {
        super.onCleared()
        stopRecording()
        audx.close()
    }

    private fun onVoiceDetected(probability: Float) {
        // Handle voice detection
    }

    private fun processCleanAudio(audio: ShortArray, size: Int) {
        // Play, save, or stream the denoised audio
    }
}
```

### Async Processing with Coroutines

```kotlin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class AudioProcessor {
    private val audx = Audx.Builder()
        .inputRate(16000)
        .resampleQuality(Audx.AUDX_RESAMPLER_QUALITY_DEFAULT)
        .build()

    suspend fun processAudioFile(inputFile: File): Flow<ProcessedChunk> = flow {
        val audioData = readAudioFile(inputFile) // Your file reader
        val chunkSize = 160 // 10ms at 16kHz

        for (i in audioData.indices step chunkSize) {
            val chunk = audioData.sliceArray(i until minOf(i + chunkSize, audioData.size))
            val output = ShortArray(chunk.size)

            audx.processAsync(chunk, output) { vadProbability ->
                emit(ProcessedChunk(output, vadProbability))
            }
        }
    }

    fun close() {
        audx.close()
    }
}

data class ProcessedChunk(val audio: ShortArray, val vadProbability: Float)
```

## API Reference

### Builder Configuration

```kotlin
val audx = Audx.Builder()
    .inputRate(sampleRate)      // Input/output sample rate (Hz)
    .resampleQuality(quality)    // Resampler quality: 0-10
    .build()
```

#### Sample Rates
48kHz by default, if your audio rate not 48kHz, you must specify your audio sample rate via `inputRate(your sample rate)`. Any positive sample rate is supported (e.g., 8000, 16000, 24000, 48000)

#### Resampler Quality Constants
- `AUDX_RESAMPLER_QUALITY_MIN` (0) - Fastest, lowest quality
- `AUDX_RESAMPLER_QUALITY_VOIP` (3) - Optimized for real-time voice
- `AUDX_RESAMPLER_QUALITY_DEFAULT` (4) - Balanced quality/performance
- `AUDX_RESAMPLER_QUALITY_MAX` (10) - Highest quality, slowest

### Processing Methods

```kotlin
// Synchronous processing
fun process(
    input: ShortArray,
    output: ShortArray,
    vadProbabilityCallback: (Float) -> Unit
)

// Asynchronous processing (coroutine)
suspend fun processAsync(
    input: ShortArray,
    output: ShortArray,
    vadProbabilityCallback: (Float) -> Unit
)
```

### Resource Management

```kotlin
fun close()           // Release native resources
fun isClosed(): Boolean  // Check if instance is closed
```

⚠️ **Important**: Always call `close()` when done to free native resources.

## Performance Tips

1. **Choose appropriate quality**: Use `AUDX_RESAMPLER_QUALITY_VOIP` for real-time applications
2. **Reuse buffers**: Allocate output buffers once and reuse them
3. **Match sample rates**: If possible, use 48kHz to avoid resampling overhead

## Supported Platforms

- **Minimum SDK**: Android 24 (Android 7.0)
- **Architectures**: ARM64 (arm64-v8a), ARMv7 (armeabi-v7a, NEON required), x86_64
- **NDK**: Built with CMake 3.22.1

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Support

If you encounter any issues or have questions:
- Open an issue on [GitHub](https://github.com/rizukirr/audx-android/issues)
