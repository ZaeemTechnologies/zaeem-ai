package com.zaeemtechnologies.ai

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class Prov(val id: String, val label: String, val url: String, val defModel: String, val keyHelp: String)

/** Only the provider the user selects is ever contacted. No silent fallback. */
val PROVS = listOf(
    Prov("groq", "Groq", "https://api.groq.com/openai/v1/chat/completions", "llama-3.3-70b-versatile", "console.groq.com/keys"),
    Prov("gemini", "Gemini", "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions", "gemini-2.0-flash", "aistudio.google.com/apikey"),
    Prov("openrouter", "OpenRouter", "https://openrouter.ai/api/v1/chat/completions", "meta-llama/llama-3.3-70b-instruct:free", "openrouter.ai/keys"))

class LlmError(msg: String) : Exception(msg)

object Llm {
    @Volatile var cancelled = false
    fun stream(p: Prov, key: String, model: String, msgs: List<Pair<String, String>>, onDelta: (String) -> Unit) {
        val arr = JSONArray()
        msgs.forEach { arr.put(JSONObject().put("role", it.first).put("content", it.second)) }
        val body = JSONObject().put("model", model).put("messages", arr).put("stream", true).toString()
        val c = URL(p.url).openConnection() as HttpURLConnection
        try {
            c.requestMethod = "POST"; c.connectTimeout = 15000; c.readTimeout = 60000; c.doOutput = true
            c.setRequestProperty("Authorization", "Bearer $key")
            c.setRequestProperty("Content-Type", "application/json")
            c.outputStream.use { it.write(body.toByteArray()) }
            val code = c.responseCode
            if (code != 200) {
                val e = try { c.errorStream?.bufferedReader()?.readText() ?: "" } catch (x: Exception) { "" }
                throw LlmError(when (code) {
                    401, 403 -> "Invalid or unauthorized API key (HTTP $code)."
                    429 -> "Rate limit or free quota reached (HTTP 429). Try later or pick another provider in Settings."
                    404 -> "Model not found: $model. Change the model name in Settings."
                    else -> "Provider error HTTP $code ${e.take(160)}"
                })
            }
            c.inputStream.bufferedReader().use { r ->
                while (!cancelled) {
                    val line = r.readLine() ?: break
                    if (!line.startsWith("data:")) continue
                    val d = line.substring(5).trim()
                    if (d == "[DONE]") break
                    val delta = JSONObject(d).optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("delta")
                    if (delta != null && !delta.isNull("content")) onDelta(delta.getString("content"))
                }
            }
        } catch (e: IOException) {
            if (!cancelled) throw LlmError("Network error: ${e.message ?: "connection failed"}")
        } finally { c.disconnect() }
    }
}
