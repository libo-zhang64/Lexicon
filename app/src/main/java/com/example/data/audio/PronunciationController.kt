package com.example.data.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

private const val TAG = "PronunciationController"

class PronunciationController(context: Context) {

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _speakingItemId = MutableStateFlow<String?>(null)
    val speakingItemId: StateFlow<String?> = _speakingItemId.asStateFlow()

    private val _isSlowSpeed = MutableStateFlow(false)
    val isSlowSpeed: StateFlow<Boolean> = _isSlowSpeed.asStateFlow()

    private var tts: TextToSpeech? = null

    init {
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = tts?.setLanguage(Locale.US)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.w(TAG, "US English TTS voice data missing; falling back to default locale.")
                    }
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _speakingItemId.value = utteranceId
                        }

                        override fun onDone(utteranceId: String?) {
                            if (_speakingItemId.value == utteranceId) {
                                _speakingItemId.value = null
                            }
                        }

                        @Deprecated("Deprecated in Android API")
                        override fun onError(utteranceId: String?) {
                            if (_speakingItemId.value == utteranceId) {
                                _speakingItemId.value = null
                            }
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            if (_speakingItemId.value == utteranceId) {
                                _speakingItemId.value = null
                            }
                        }
                    })
                    _isReady.value = true
                } else {
                    Log.e(TAG, "TextToSpeech initialization failed with status=$status")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to construct TextToSpeech", e)
        }
    }

    fun toggleSpeed() {
        _isSlowSpeed.value = !_isSlowSpeed.value
    }

    fun speak(itemId: String, text: String) {
        val engine = tts ?: return
        if (!_isReady.value) return
        if (_speakingItemId.value == itemId) {
            stop()
            return
        }
        val cleanText = text.replace("(", "").replace(")", "").trim()
        if (cleanText.isEmpty()) return
        val rate = if (_isSlowSpeed.value) 0.75f else 1.0f
        engine.setSpeechRate(rate)
        _speakingItemId.value = itemId
        engine.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, itemId)
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (_: Exception) {
        }
        _speakingItemId.value = null
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {
        }
        tts = null
        _isReady.value = false
        _speakingItemId.value = null
    }
}
