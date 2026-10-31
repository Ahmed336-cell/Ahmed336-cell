package com.athar.mobile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.athar.engine.AnalyzeResult
import com.athar.engine.LookupResult
import com.athar.engine.Matcher
import com.athar.mobile.asr.OnDeviceWhisperAsr
import com.athar.mobile.data.AssetCorpusLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AtharUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val corpusLabel: String = "",
    val query: String = "",
    val lookup: LookupResult? = null,
    val analyze: AnalyzeResult? = null,
    val asrReady: Boolean = false,
    val asrHasModel: Boolean = false,
    val asrNote: String = "",
    val busy: Boolean = false
)

class AtharViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow(AtharUiState())
    val state: StateFlow<AtharUiState> = _state.asStateFlow()

    private var matcher: Matcher? = null
    private var asr: OnDeviceWhisperAsr? = null

    init {
        viewModelScope.launch {
            try {
                val corpus = withContext(Dispatchers.Default) {
                    AssetCorpusLoader.load(app)
                }
                matcher = Matcher(corpus)
                asr = OnDeviceWhisperAsr(app)
                _state.update {
                    it.copy(
                        loading = false,
                        corpusLabel = "قرآن ${corpus.quranCount} · حديث ${corpus.hadithCount}",
                        asrReady = asr?.isReady == true,
                        asrHasModel = asr?.hasBinaryModel == true,
                        asrNote = if (asr?.hasBinaryModel == true) {
                            "نموذج Whisper جاهز على الجهاز"
                        } else {
                            "ضع whisper-small-ar.onnx في assets/models للتفريغ دون إنترنت"
                        }
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(loading = false, error = e.message ?: "فشل تحميل المكتبة")
                }
            }
        }
    }

    fun onQueryChange(value: String) {
        _state.update { it.copy(query = value) }
    }

    fun lookup() {
        val m = matcher ?: return
        val q = _state.value.query
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            val result = withContext(Dispatchers.Default) { m.lookup(q) }
            _state.update { it.copy(busy = false, lookup = result, analyze = null) }
        }
    }

    fun analyzeLecture() {
        val m = matcher ?: return
        val q = _state.value.query
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            val result = withContext(Dispatchers.Default) { m.analyze(q) }
            _state.update { it.copy(busy = false, analyze = result, lookup = null) }
        }
    }

    fun loadDemo() {
        _state.update {
            it.copy(
                query = "قال الله تعالى الحمد لله رب العالمين وقال رسول الله صلى الله عليه وسلم انما الاعمال بالنيات وانما لكل امرئ ما نوي"
            )
        }
    }
}
