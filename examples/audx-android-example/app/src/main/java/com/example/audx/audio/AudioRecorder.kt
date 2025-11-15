package com.example.audx.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.android.audx.AudxDenoiser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

/**
 * Audio recorder that captures audio at 16kHz for use with AudxDenoiser's resampling feature
 *
 * This class demonstrates using a non-48kHz sample rate with audx-android:
 * - Uses 16kHz sample rate (will be resampled by AudxDenoiser to 48kHz)
 * - Uses AudxDenoiser.CHANNELS (1 for mono)
 * - Uses AudxDenoiser.BIT_DEPTH (16-bit)
 * - Calculates appropriate buffer sizes for 16kHz
 * - AudxDenoiser will handle automatic resampling from 16kHz to 48kHz
 */
class AudioRecorder {

    companion object {
        private const val TAG = "AudioRecorder"

        /**
         * 16kHz, in this case AudxDenoiser will auto resampler to 48kHz as requirements
         * but we need to initialize AudxDenoiser with .inputSampleRate(16000) to
         * acknowledge AudxDenoiser to resampling from 16kHz
         * */
        const val SAMPLE_RATE = 16000 // 16kHz - will be resampled by AudxDenoiser

        /**
         * AudxDenoiser require 10ms chunk so 16000 sample/sec * 0.01 = 160 samples
         */
        const val FRAME_SIZE = 160
    }

    private var audioRecord: AudioRecord? = null

    /**
     * Initialize AudioRecord with 16kHz sample rate
     *
     * Records at 16kHz, which will be automatically resampled by AudxDenoiser to 48kHz.
     */
    @SuppressLint("MissingPermission")
    fun initialize(): Result<Unit> {
        return try {
            // Calculate buffer size for 16kHz
            // Using 40ms buffer for stable recording and prevent underruns
            // At 16kHz: 40ms = 16000 samples/sec * 0.04 sec = 640 samples
            // Buffer size in bytes = samples * 2 (16-bit = 2 bytes per sample)
            val bufferSizeInSamples = FRAME_SIZE * 4 // 640 samples at 16kHz
            val bufferSize = bufferSizeInSamples * 2 // Convert to bytes

            Log.i(TAG, "Initializing AudioRecord with:")
            Log.i(TAG, "  Sample Rate: $SAMPLE_RATE Hz (will be resampled to 48kHz)")
            Log.i(TAG, "  Channels: ${AudxDenoiser.CHANNELS} (Mono)")
            Log.i(TAG, "  Bit Depth: ${AudxDenoiser.BIT_DEPTH}-bit PCM")
            Log.i(TAG, "  Buffer Size: $bufferSize bytes ($bufferSizeInSamples samples)")
            Log.i(TAG, "  Note: AudxDenoiser will resample from 16kHz to 48kHz")

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                return Result.failure(IllegalStateException("AudioRecord initialization failed"))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AudioRecord", e)
            Result.failure(e)
        }
    }

    /**
     * Start recording and emit audio chunks as Flow
     *
     * Captures audio at 16kHz in chunks suitable for processing.
     * Each chunk will be approximately 20ms of audio (320 samples at 16kHz).
     */
    fun startRecording(): Flow<ShortArray> = flow {
        val record = audioRecord ?: throw IllegalStateException("AudioRecord not initialized")

        // At 16kHz: 20ms = 320 samples
        // This provides a good balance between latency and efficiency
        val bufferSize = FRAME_SIZE * 2 // 320 samples
        val buffer = ShortArray(bufferSize)

        record.startRecording()
        Log.i(TAG, "Recording started at $SAMPLE_RATE Hz")

        try {
            while (currentCoroutineContext().isActive && record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                val samplesRead = record.read(buffer, 0, buffer.size)

                if (samplesRead > 0) {
                    // Emit the audio chunk (will be resampled by AudxDenoiser)
                    val chunk = buffer.copyOf(samplesRead)
                    emit(chunk)
                } else if (samplesRead < 0) {
                    Log.e(TAG, "AudioRecord read error: $samplesRead")
                    break
                }
            }
        } finally {
            // Stop recording if still active
            if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                record.stop()
                Log.i(TAG, "Recording stopped in finally block")
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Release AudioRecord resources
     */
    fun release() {
        audioRecord?.apply {
            try {
                if (state == AudioRecord.STATE_INITIALIZED) {
                    if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                        stop()
                    }
                    release()
                }
            } catch (e: IllegalStateException) {
                // AudioRecord was already stopped/released - safe to ignore
                Log.w(TAG, "AudioRecord already stopped: ${e.message}")
            }
        }
        audioRecord = null
        Log.i(TAG, "AudioRecord released")
    }
}
