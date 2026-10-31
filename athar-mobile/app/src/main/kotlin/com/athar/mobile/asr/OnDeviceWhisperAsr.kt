package com.athar.mobile.asr

import android.content.Context
import com.athar.engine.AsrModel
import com.athar.engine.AsrResult
import com.athar.engine.StubAsrModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * On-device Whisper hook.
 *
 * Drop a converted model at `assets/models/whisper-small-ar.onnx` (or ggml via JNI later).
 * Until the binary is present, falls back to [StubAsrModel] so matching still works on typed text.
 *
 * Integration path:
 * 1. Export Whisper (small / tiny) ONNX with Arabic fine-tune OR whisper.cpp ggml.
 * 2. Implement native decode here (onnxruntime-android / whisper.cpp AAR).
 * 3. Keep feeding words into [com.athar.engine.Matcher] — model never grades.
 */
class OnDeviceWhisperAsr(
    context: Context,
    private val assetModelName: String = "models/whisper-small-ar.onnx"
) : AsrModel {
    private val appContext = context.applicationContext
    private val modelFile: File? = prepareModelFile()
    private val stub = StubAsrModel()

    override val name: String =
        if (modelFile != null) "whisper-ondevice" else stub.name

    override val isReady: Boolean = true

    val hasBinaryModel: Boolean get() = modelFile != null

    override suspend fun transcribe(pcm16kMono: FloatArray, sampleRateHz: Int): AsrResult =
        withContext(Dispatchers.Default) {
            if (modelFile == null) {
                return@withContext stub.transcribe(pcm16kMono, sampleRateHz).copy(
                    text = "",
                    modelName = "stub-asr (add assets/$assetModelName for offline Whisper)"
                )
            }
            // Placeholder for ONNX / whisper.cpp inference.
            // Real decode goes here once the model binary is packaged.
            AsrResult(
                text = "",
                modelName = name,
                elapsedMs = 0
            )
        }

    private fun prepareModelFile(): File? {
        return try {
            val out = File(appContext.filesDir, File(assetModelName).name)
            if (out.exists() && out.length() > 0) return out
            appContext.assets.open(assetModelName).use { input ->
                out.outputStream().use { output -> input.copyTo(output) }
            }
            out.takeIf { it.exists() && it.length() > 0 }
        } catch (_: Exception) {
            null
        }
    }
}
