package com.aura.app.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aura.brain.BrainEngine
import com.aura.brain.ChatMessage
import com.aura.voice.VoiceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UiMessage(val role: String, val text: String)

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    private val brain = BrainEngine()
    private val voice = VoiceManager(
        context = app,
        onWakeWordDetected = { startListeningTurn() },
        onFinalTranscript = { transcript -> handleUserUtterance(transcript) },
        onPartialTranscript = { /* Phase 2: live captions */ }
    )

    private val history = mutableListOf<ChatMessage>()

    private val _messages = MutableStateFlow<List<UiMessage>>(emptyList())
    val messages: StateFlow<List<UiMessage>> = _messages.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    fun onMicPermissionGranted() {
        voice.initialize()
        voice.startWakeWordListening()
    }

    fun toggleMic() {
        if (_isListening.value) {
            voice.stopListening()
            _isListening.value = false
        } else {
            startListeningTurn()
        }
    }

    private fun startListeningTurn() {
        voice.stopSpeaking() // barge-in: interrupt AURA if she's talking
        _isSpeaking.value = false
        _isListening.value = true
        voice.startActiveListening()
    }

    private fun handleUserUtterance(transcript: String) {
        _isListening.value = false
        if (transcript.isBlank()) return
        sendText(transcript)
    }

    fun sendText(text: String) {
        appendMessage("user", text)
        history.add(ChatMessage(role = "user", content = text))

        viewModelScope.launch {
            val responseBuilder = StringBuilder()
            appendMessage("assistant", "") // placeholder bubble we stream into
            _isSpeaking.value = true

            brain.streamReply(history) { deltaToken ->
                responseBuilder.append(deltaToken)
                updateLastMessage(responseBuilder.toString())
                voice.speakStreamingChunk(deltaToken)
            }

            history.add(ChatMessage(role = "assistant", content = responseBuilder.toString()))
            voice.finishSpeaking()
            _isSpeaking.value = false

            // Resume passive wake-word listening after AURA finishes speaking
            voice.startWakeWordListening()
        }
    }

    private fun appendMessage(role: String, text: String) {
        _messages.value = _messages.value + UiMessage(role, text)
    }

    private fun updateLastMessage(text: String) {
        val current = _messages.value.toMutableList()
        if (current.isNotEmpty()) {
            current[current.lastIndex] = current.last().copy(text = text)
            _messages.value = current
        }
    }

    override fun onCleared() {
        super.onCleared()
        voice.release()
    }
}
