package com.aura.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID

/**
 * Owns the voice I/O loop for AURA:
 *  1. Passive wake-word listening ("AURA")
 *  2. Active listening for a full user utterance once woken
 *  3. Streaming TTS with barge-in (stopSpeaking() cancels mid-sentence)
 *
 * IMPLEMENTATION NOTE (read before shipping):
 * Android's built-in SpeechRecognizer has no dedicated "wake word" mode — it
 * does full speech recognition. This class approximates wake-word behavior by
 * restarting short recognition sessions and checking the transcript for the
 * wake phrase. This works but drains more battery than a real keyword spotter.
 *
 * RECOMMENDATION for Phase 2: replace the passive-listening path with
 * Picovoice Porcupine (on-device, purpose-built wake-word engine, ~1% battery
 * impact, offline, free tier available). Swap only startWakeWordListening()/
 * stopListening() internals — the public API below stays the same, so nothing
 * upstream (ChatViewModel) needs to change.
 */
class VoiceManager(
    private val context: Context,
    private val wakeWord: String = "aura",
    private val onWakeWordDetected: () -> Unit,
    private val onFinalTranscript: (String) -> Unit,
    private val onPartialTranscript: (String) -> Unit = {}
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var mode: Mode = Mode.IDLE

    private enum class Mode { IDLE, PASSIVE_WAKE, ACTIVE_LISTEN }

    fun initialize() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        }
        tts = TextToSpeech(context) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            tts?.language = Locale.getDefault()
        }
    }

    // ---------- Passive wake-word listening ----------

    fun startWakeWordListening() {
        mode = Mode.PASSIVE_WAKE
        runRecognitionCycle()
    }

    private fun runRecognitionCycle() {
        val recognizer = speechRecognizer ?: return
        val intent = buildRecognizerIntent(preferOffline = true)

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle) {
                val text = firstResult(results)
                when (mode) {
                    Mode.PASSIVE_WAKE -> {
                        if (text.lowercase(Locale.getDefault()).contains(wakeWord)) {
                            onWakeWordDetected()
                        } else {
                            runRecognitionCycle() // keep listening passively
                        }
                    }
                    Mode.ACTIVE_LISTEN -> onFinalTranscript(text)
                    Mode.IDLE -> { /* no-op */ }
                }
            }

            override fun onPartialResults(partialResults: Bundle) {
                onPartialTranscript(firstResult(partialResults))
            }

            override fun onError(error: Int) {
                // Timeouts are expected/frequent in passive mode; just restart.
                if (mode == Mode.PASSIVE_WAKE) runRecognitionCycle()
                else if (mode == Mode.ACTIVE_LISTEN) onFinalTranscript("")
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        recognizer.startListening(intent)
    }

    // ---------- Active listening (full command) ----------

    fun startActiveListening() {
        mode = Mode.ACTIVE_LISTEN
        speechRecognizer?.cancel()
        runRecognitionCycle()
    }

    fun stopListening() {
        mode = Mode.IDLE
        speechRecognizer?.stopListening()
    }

    private fun buildRecognizerIntent(preferOffline: Boolean): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, preferOffline)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }

    private fun firstResult(bundle: Bundle): String =
        bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()

    // ---------- TTS with barge-in support ----------

    fun speakStreamingChunk(text: String) {
        if (!ttsReady || text.isBlank()) return
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, UUID.randomUUID().toString())
    }

    fun finishSpeaking() {
        // No-op placeholder for Phase 1: QUEUE_ADD already plays chunks in order.
        // Phase 2 will add end-of-turn chime + sentence-boundary chunking for
        // lower perceived latency.
    }

    fun stopSpeaking() {
        tts?.stop()
    }

    fun release() {
        speechRecognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
    }
}
