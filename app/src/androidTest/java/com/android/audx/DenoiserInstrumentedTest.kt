package com.android.audx

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Instrumented tests for the Denoiser library.
 * These tests run on an Android device and verify the native library integration.
 *
 * The tests use noise_audio.pcm from raw resources which is 48kHz 16-bit PCM audio.
 */
@RunWith(AndroidJUnit4::class)
class DenoiserInstrumentedTest {

    private var denoiser: Denoiser? = null
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        // Ensure any previous denoiser is cleaned up
        denoiser?.destroy()
        denoiser = null
    }

    @After
    fun tearDown() {
        denoiser?.destroy()
        denoiser = null
    }

    // ==================== Initialization Tests ====================

    @Test
    fun testBuilderWithDefaultSettings() {
        denoiser = Denoiser.Builder()
            .numChannels(1)
            .build()

        assertNotNull("Denoiser should be created", denoiser)
    }

    @Test
    fun testBuilderMonoConfiguration() {
        denoiser = Denoiser.Builder()
            .numChannels(1)
            .vadThreshold(0.5f)
            .enableVadOutput(true)
            .build()

        assertNotNull("Mono denoiser should be created", denoiser)
    }

    @Test
    fun testBuilderStereoConfiguration() {
        denoiser = Denoiser.Builder()
            .numChannels(2)
            .vadThreshold(0.5f)
            .enableVadOutput(true)
            .build()

        assertNotNull("Stereo denoiser should be created", denoiser)
    }

    @Test
    fun testBuilderCustomVadThreshold() {
        denoiser = Denoiser.Builder()
            .numChannels(1)
            .vadThreshold(0.7f)
            .build()

        assertNotNull("Denoiser with custom VAD threshold should be created", denoiser)
    }

    @Test
    fun testBuilderVadDisabled() {
        denoiser = Denoiser.Builder()
            .numChannels(1)
            .enableVadOutput(false)
            .build()

        assertNotNull("Denoiser with VAD disabled should be created", denoiser)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInvalidChannelCount_Zero() {
        denoiser = Denoiser.Builder()
            .numChannels(0)
            .build()
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInvalidChannelCount_Three() {
        denoiser = Denoiser.Builder()
            .numChannels(3)
            .build()
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInvalidVadThreshold_Negative() {
        denoiser = Denoiser.Builder()
            .numChannels(1)
            .vadThreshold(-0.1f)
            .build()
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInvalidVadThreshold_TooHigh() {
        denoiser = Denoiser.Builder()
            .numChannels(1)
            .vadThreshold(1.5f)
            .build()
    }

    // ==================== Constants Tests ====================

    @Test
    fun testConstants() {
        assertEquals("Sample rate should be 48000", 48000, Denoiser.SAMPLE_RATE)
        assertEquals("Frame size should be 480", 480, Denoiser.FRAME_SIZE)
        assertEquals("Frame duration should be 10ms", 10, Denoiser.getFrameDurationMs())
    }

    @Test
    fun testGetFrameSizeInSamples() {
        assertEquals("Mono frame size should be 480", 480, Denoiser.getFrameSizeInSamples(1))
        assertEquals("Stereo frame size should be 960", 960, Denoiser.getFrameSizeInSamples(2))
    }

    @Test
    fun testGetRecommendedBufferSize() {
        // For mono, 10ms at 48kHz: 48000 * 0.01 * 1 * 2 bytes = 960 bytes
        assertEquals("Mono buffer size for 10ms", 960, Denoiser.getRecommendedBufferSize(1, 10))

        // For stereo, 10ms at 48kHz: 48000 * 0.01 * 2 * 2 bytes = 1920 bytes
        assertEquals("Stereo buffer size for 10ms", 1920, Denoiser.getRecommendedBufferSize(2, 10))

        // For mono, 20ms at 48kHz: 48000 * 0.02 * 1 * 2 bytes = 1920 bytes
        assertEquals("Mono buffer size for 20ms", 1920, Denoiser.getRecommendedBufferSize(1, 20))
    }

    // ==================== Audio Processing Tests ====================

    @Test
    fun testProcessChunk_SingleFrame() = runBlocking {
        val audioData = loadPcmAudioFromRaw(R.raw.noise_audio)
        val frameSize = Denoiser.FRAME_SIZE // 480 samples for mono

        var callbackInvoked = false
        var receivedAudio: ShortArray? = null
        var receivedResult: DenoiserResult? = null

        denoiser = Denoiser.Builder()
            .numChannels(1)
            .vadThreshold(0.5f)
            .onProcessedAudio { audio, result ->
                callbackInvoked = true
                receivedAudio = audio
                receivedResult = result
            }
            .build()

        // Process first frame
        val firstFrame = audioData.copyOfRange(0, frameSize)
        denoiser?.processChunk(firstFrame)

        assertTrue("Callback should be invoked", callbackInvoked)
        assertNotNull("Received audio should not be null", receivedAudio)
        assertNotNull("Received result should not be null", receivedResult)
        assertEquals("Output size should match frame size", frameSize, receivedAudio?.size)
    }

    @Test
    fun testProcessChunk_MultipleFrames() = runBlocking {
        val audioData = loadPcmAudioFromRaw(R.raw.noise_audio)
        val frameSize = Denoiser.FRAME_SIZE
        val numFramesToProcess = 10

        var callbackCount = 0

        denoiser = Denoiser.Builder()
            .numChannels(1)
            .vadThreshold(0.5f)
            .onProcessedAudio { _, _ ->
                callbackCount++
            }
            .build()

        // Process 10 frames
        for (i in 0 until numFramesToProcess) {
            val start = i * frameSize
            val end = minOf(start + frameSize, audioData.size)
            if (end - start == frameSize) {
                val frame = audioData.copyOfRange(start, end)
                denoiser?.processChunk(frame)
            }
        }

        assertEquals("Callback should be invoked 10 times", numFramesToProcess, callbackCount)
    }

    @Test
    fun testProcessChunk_VadProbabilityRange() = runBlocking {
        val audioData = loadPcmAudioFromRaw(R.raw.noise_audio)
        val frameSize = Denoiser.FRAME_SIZE

        val vadProbabilities = mutableListOf<Float>()

        denoiser = Denoiser.Builder()
            .numChannels(1)
            .vadThreshold(0.5f)
            .onProcessedAudio { _, result ->
                vadProbabilities.add(result.vadProbability)
            }
            .build()

        // Process first 10 frames
        for (i in 0 until 10) {
            val start = i * frameSize
            val end = start + frameSize
            if (end <= audioData.size) {
                val frame = audioData.copyOfRange(start, end)
                denoiser?.processChunk(frame)
            }
        }

        assertTrue("Should have VAD probabilities", vadProbabilities.isNotEmpty())
        vadProbabilities.forEach { prob ->
            assertTrue("VAD probability should be >= 0.0: $prob", prob >= 0.0f)
            assertTrue("VAD probability should be <= 1.0: $prob", prob <= 1.0f)
        }
    }

    @Test
    fun testProcessChunk_SamplesProcessedCount() = runBlocking {
        val audioData = loadPcmAudioFromRaw(R.raw.noise_audio)
        val frameSize = Denoiser.FRAME_SIZE

        var samplesProcessed = 0

        denoiser = Denoiser.Builder()
            .numChannels(1)
            .vadThreshold(0.5f)
            .onProcessedAudio { _, result ->
                samplesProcessed = result.samplesProcessed
            }
            .build()

        val firstFrame = audioData.copyOfRange(0, frameSize)
        denoiser?.processChunk(firstFrame)

        assertEquals("Samples processed should be 480 for mono", frameSize, samplesProcessed)
    }

    @Test
    fun testProcessChunk_OutputNotAllZeros() = runBlocking {
        val audioData = loadPcmAudioFromRaw(R.raw.noise_audio)
        val frameSize = Denoiser.FRAME_SIZE

        var lastReceivedAudio: ShortArray? = null
        var frameCount = 0

        denoiser = Denoiser.Builder()
            .numChannels(1)
            .vadThreshold(0.5f)
            .onProcessedAudio { audio, _ ->
                lastReceivedAudio = audio
                frameCount++
            }
            .build()

        // Process first 3 frames to allow denoiser to warm up
        for (i in 0 until 3) {
            val start = i * frameSize
            val end = start + frameSize
            if (end <= audioData.size) {
                val frame = audioData.copyOfRange(start, end)
                denoiser?.processChunk(frame)
            }
        }

        assertEquals("Should have processed 3 frames", 3, frameCount)
        assertNotNull("Received audio should not be null", lastReceivedAudio)

        // Check that at least one of the processed frames has non-zero output
        val hasNonZero = lastReceivedAudio?.any { it != 0.toShort() } ?: false
        assertTrue("Output should contain non-zero values after warmup", hasNonZero)
    }

    // ==================== Streaming Mode Tests ====================

    @Test
    fun testProcessChunk_SmallChunks_Buffering() = runBlocking {
        val audioData = loadPcmAudioFromRaw(R.raw.noise_audio)
        val chunkSize = 100 // Smaller than frame size (480)

        var callbackCount = 0

        denoiser = Denoiser.Builder()
            .numChannels(1)
            .vadThreshold(0.5f)
            .onProcessedAudio { _, _ ->
                callbackCount++
            }
            .build()

        // Process multiple small chunks
        val numChunks = 10
        for (i in 0 until numChunks) {
            val start = i * chunkSize
            val end = minOf(start + chunkSize, audioData.size)
            val chunk = audioData.copyOfRange(start, end)
            denoiser?.processChunk(chunk)
        }

        // Total samples = 1000, which gives 2 complete frames (960 samples)
        assertEquals("Should process 2 complete frames", 2, callbackCount)
    }

    @Test
    fun testProcessChunk_LargeChunks() = runBlocking {
        val audioData = loadPcmAudioFromRaw(R.raw.noise_audio)
        val largeChunkSize = 2000 // Larger than one frame

        var callbackCount = 0

        denoiser = Denoiser.Builder()
            .numChannels(1)
            .vadThreshold(0.5f)
            .onProcessedAudio { _, _ ->
                callbackCount++
            }
            .build()

        val chunk = audioData.copyOfRange(0, minOf(largeChunkSize, audioData.size))
        denoiser?.processChunk(chunk)

        // 2000 samples should produce 4 complete frames (4 * 480 = 1920)
        assertEquals("Should process 4 frames from large chunk", 4, callbackCount)
    }

    @Test
    fun testFlush_WithRemainingData() = runBlocking {
        val partialFrameSize = 240 // Half a frame

        var callbackCount = 0
        var lastAudioSize = 0

        denoiser = Denoiser.Builder()
            .numChannels(1)
            .vadThreshold(0.5f)
            .onProcessedAudio { audio, _ ->
                callbackCount++
                lastAudioSize = audio.size
            }
            .build()

        // Create partial frame
        val partialData = ShortArray(partialFrameSize) { (it % 100).toShort() }
        denoiser?.processChunk(partialData)

        // Should not trigger callback yet (incomplete frame)
        assertEquals("Should not process incomplete frame", 0, callbackCount)

        // Flush should process the remaining data
        denoiser?.flush()

        assertEquals("Flush should trigger callback", 1, callbackCount)
        assertEquals("Last audio size should be the partial frame size", partialFrameSize, lastAudioSize)
    }

    @Test
    fun testFlush_NoRemainingData() = runBlocking {
        val frameSize = Denoiser.FRAME_SIZE

        var callbackCount = 0

        denoiser = Denoiser.Builder()
            .numChannels(1)
            .vadThreshold(0.5f)
            .onProcessedAudio { _, _ ->
                callbackCount++
            }
            .build()

        // Process complete frame
        val completeFrame = ShortArray(frameSize) { (it % 100).toShort() }
        denoiser?.processChunk(completeFrame)

        assertEquals("Should process 1 frame", 1, callbackCount)

        // Flush with no remaining data
        denoiser?.flush()

        // Should still be 1 (no additional callback)
        assertEquals("Flush should not trigger additional callback", 1, callbackCount)
    }

    @Test
    fun testProcessChunk_VariableSizeChunks() = runBlocking {
        val audioData = loadPcmAudioFromRaw(R.raw.noise_audio)

        var callbackCount = 0
        val chunkSizes = listOf(100, 300, 450, 200, 500)

        denoiser = Denoiser.Builder()
            .numChannels(1)
            .vadThreshold(0.5f)
            .onProcessedAudio { _, _ ->
                callbackCount++
            }
            .build()

        var offset = 0
        chunkSizes.forEach { size ->
            if (offset + size <= audioData.size) {
                val chunk = audioData.copyOfRange(offset, offset + size)
                denoiser?.processChunk(chunk)
                offset += size
            }
        }

        // Total: 100 + 300 + 450 + 200 + 500 = 1550 samples
        // Complete frames: 1550 / 480 = 3 frames (1440 samples)
        assertEquals("Should process 3 complete frames", 3, callbackCount)
    }

    // ==================== Resource Management Tests ====================

    @Test
    fun testDestroy() {
        denoiser = Denoiser.Builder()
            .numChannels(1)
            .build()

        assertNotNull("Denoiser should be created", denoiser)

        denoiser?.destroy()

        // Attempting to use after destroy should fail
        try {
            runBlocking {
                denoiser?.processChunk(ShortArray(480))
            }
            fail("Should throw exception when using destroyed denoiser")
        } catch (e: IllegalStateException) {
            assertTrue("Should throw IllegalStateException", e.message?.contains("destroyed") == true)
        }
    }

    @Test
    fun testAutoCloseable() {
        var denoisedSamples = 0

        Denoiser.Builder()
            .numChannels(1)
            .onProcessedAudio { audio, _ ->
                denoisedSamples += audio.size
            }
            .build().use { denoiser ->
                runBlocking {
                    val frame = ShortArray(480) { (it % 100).toShort() }
                    denoiser.processChunk(frame)
                }
            }

        assertEquals("Should process frame before closing", 480, denoisedSamples)
    }

    @Test
    fun testMultipleDestroyCalls() {
        denoiser = Denoiser.Builder()
            .numChannels(1)
            .build()

        denoiser?.destroy()
        // Second destroy should not crash
        denoiser?.destroy()
    }

    // ==================== Stereo Tests ====================

    @Test
    fun testStereoProcessing_FrameSize() = runBlocking {
        val stereoFrameSize = Denoiser.FRAME_SIZE * 2 // 960 for stereo

        var receivedAudioSize = 0

        denoiser = Denoiser.Builder()
            .numChannels(2)
            .vadThreshold(0.5f)
            .onProcessedAudio { audio, _ ->
                receivedAudioSize = audio.size
            }
            .build()

        // Create interleaved stereo frame [L, R, L, R, ...]
        val stereoFrame = ShortArray(stereoFrameSize) { (it % 200).toShort() }
        denoiser?.processChunk(stereoFrame)

        assertEquals("Stereo output size should be 960", stereoFrameSize, receivedAudioSize)
    }

    @Test
    fun testStereoProcessing_MultipleFrames() = runBlocking {
        val stereoFrameSize = Denoiser.FRAME_SIZE * 2

        var callbackCount = 0

        denoiser = Denoiser.Builder()
            .numChannels(2)
            .vadThreshold(0.5f)
            .onProcessedAudio { _, _ ->
                callbackCount++
            }
            .build()

        // Process 5 stereo frames
        repeat(5) {
            val stereoFrame = ShortArray(stereoFrameSize) { (it % 200).toShort() }
            denoiser?.processChunk(stereoFrame)
        }

        assertEquals("Should process 5 stereo frames", 5, callbackCount)
    }

    // ==================== Error Cases ====================

    @Test(expected = IllegalArgumentException::class)
    fun testProcessChunk_WithoutCallback() {
        denoiser = Denoiser.Builder()
            .numChannels(1)
            // No callback set
            .build()

        runBlocking {
            val frame = ShortArray(480)
            denoiser?.processChunk(frame)
        }
    }

    @Test
    fun testIsVoiceDetected() {
        denoiser = Denoiser.Builder()
            .numChannels(1)
            .build()

        assertTrue("Should detect voice at 0.6 with threshold 0.5",
            denoiser?.isVoiceDetected(0.6f, 0.5f) == true)

        assertFalse("Should not detect voice at 0.3 with threshold 0.5",
            denoiser?.isVoiceDetected(0.3f, 0.5f) == true)

        assertTrue("Should detect voice at exactly threshold",
            denoiser?.isVoiceDetected(0.5f, 0.5f) == true)
    }

    // ==================== Helper Methods ====================

    /**
     * Load PCM audio data from raw resources and convert to ShortArray.
     * The PCM file should be 16-bit signed PCM at 48kHz.
     */
    private fun loadPcmAudioFromRaw(resourceId: Int): ShortArray {
        val inputStream: InputStream = context.resources.openRawResource(resourceId)
        val byteArray = inputStream.readBytes()
        inputStream.close()

        return convertBytesToShorts(byteArray)
    }

    /**
     * Convert byte array to short array (16-bit PCM, little-endian)
     */
    private fun convertBytesToShorts(bytes: ByteArray): ShortArray {
        val shorts = ShortArray(bytes.size / 2)
        val byteBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        for (i in shorts.indices) {
            shorts[i] = byteBuffer.getShort()
        }

        return shorts
    }
}
