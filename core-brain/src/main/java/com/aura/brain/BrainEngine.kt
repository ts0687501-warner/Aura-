package com.aura.brain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Talks to the Anthropic Messages API (streaming) and exposes token deltas
 * so the UI and TTS engine can consume them as they arrive.
 *
 * SECURITY: the API key is never hardcoded. It is read from BuildConfig,
 * which is populated at build time from a gitignored local.properties entry
 * (CLAUDE_API_KEY=sk-ant-...). See core-brain/README.md for setup.
 */
class BrainEngine(
    private val apiKey: String = ApiKeyProvider.getKey(),
    private val model: String = "claude-sonnet-4-6",
    private val systemPrompt: String = DEFAULT_SYSTEM_PROMPT
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Streams a reply for the given conversation history. Invokes [onToken]
     * for every text delta as it arrives. Suspends until the stream is done.
     */
    suspend fun streamReply(
        history: List<ChatMessage>,
        onToken: (String) -> Unit
    ): Unit = withContext(Dispatchers.IO) {
        val body = buildRequestBody(history)
        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        suspendCoroutine<Unit> { continuation ->
            var resumed = false
            val listener = object : EventSourceListener() {
                override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                    if (data == "[DONE]") return
                    runCatching {
                        val json = JSONObject(data)
                        if (json.optString("type") == "content_block_delta") {
                            val delta = json.optJSONObject("delta")
                            val text = delta?.optString("text")
                            if (!text.isNullOrEmpty()) onToken(text)
                        }
                    }
                }

                override fun onClosed(eventSource: EventSource) {
                    if (!resumed) { resumed = true; continuation.resume(Unit) }
                }

                override fun onFailure(eventSource: EventSource, t: Throwable?, response: okhttp3.Response?) {
                    if (!resumed) {
                        resumed = true
                        onToken("\n[AURA: connection error — ${t?.message ?: response?.code}]")
                        continuation.resume(Unit)
                    }
                }
            }
            EventSources.createFactory(client).newEventSource(request, listener)
        }
    }

    private fun buildRequestBody(history: List<ChatMessage>): JSONObject {
        val messages = JSONArray()
        history.forEach { msg ->
            messages.put(
                JSONObject()
                    .put("role", msg.role)
                    .put("content", msg.content)
            )
        }
        return JSONObject()
            .put("model", model)
            .put("max_tokens", 1024)
            .put("system", systemPrompt)
            .put("stream", true)
            .put("messages", messages)
    }

    companion object {
        const val DEFAULT_SYSTEM_PROMPT = """
            You are AURA, a personal AI assistant running on the user's Android phone.
            Be calm, warm, direct, and a little witty when appropriate. Keep spoken
            responses concise (this text may be read aloud by TTS) unless the user
            is asking for detailed written help. Never claim to have taken a phone
            action (opening apps, setting alarms, etc.) unless that action was
            actually executed by AURA's tool layer.
        """
    }
}

/**
 * Reads the Anthropic API key injected via BuildConfig (see app/build.gradle.kts
 * buildConfigField wiring in Phase 1 setup notes). Throws a clear error if unset
 * rather than silently failing, so first-run misconfiguration is obvious.
 */
object ApiKeyProvider {
    private var override: String? = null

    fun setKey(key: String) { override = key }

    fun getKey(): String {
        return override
            ?: throw IllegalStateException(
                "No Claude API key configured. Call ApiKeyProvider.setKey(...) at app " +
                "startup (e.g. from AuraApplication) with a key loaded from encrypted " +
                "local storage or your backend — never commit a key into source."
            )
    }
}
