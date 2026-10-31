package com.athar.engine

/**
 * Contract for on-device speech models used by Athar Mobile.
 *
 * Implementations (Android module):
 * - [StubAsrModel]: returns empty / demo transcript (no binary model required)
 * - Whisper ONNX / whisper.cpp JNI: real offline Arabic ASR
 *
 * Matching stays deterministic. ASR only produces words; it never grades hadith.
 */
interface AsrModel {
    val name: String
    val isReady: Boolean

    /** Transcribe 16 kHz mono PCM float samples. */
    suspend fun transcribe(pcm16kMono: FloatArray, sampleRateHz: Int = 16_000): AsrResult
}

data class AsrResult(
    val text: String,
    val segments: List<AsrSegment> = emptyList(),
    val modelName: String,
    val elapsedMs: Long
)

data class AsrSegment(
    val startSec: Double,
    val endSec: Double,
    val text: String
)

/** Safe default when no model file is packaged. */
class StubAsrModel(
    private val canned: String = ""
) : AsrModel {
    override val name: String = "stub-asr"
    override val isReady: Boolean = true

    override suspend fun transcribe(pcm16kMono: FloatArray, sampleRateHz: Int): AsrResult {
        val t0 = System.currentTimeMillis()
        return AsrResult(
            text = canned,
            modelName = name,
            elapsedMs = System.currentTimeMillis() - t0
        )
    }
}
