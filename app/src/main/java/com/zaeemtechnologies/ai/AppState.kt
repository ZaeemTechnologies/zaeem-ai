package com.zaeemtechnologies.ai

import android.content.Context
import androidx.compose.runtime.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

enum class EState { IDLE, LISTENING, THINKING, SPEAKING, ERROR }
data class Msg(val role: String, val text: String)
data class Chat(val id: String, val title: String, val msgs: List<Msg>)

class AppState(ctx: Context) {
    val secure = SecureStore(ctx)
    private val prefs = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val chatFile = File(ctx.filesDir, "chats.json")
    private val memFile = File(ctx.filesDir, "memory.json")

    var chats by mutableStateOf(listOf<Chat>())
    var curId by mutableStateOf("")
    var memories by mutableStateOf(listOf<String>())
    var memoryOn by mutableStateOf(prefs.getBoolean("memoryOn", false))
    var speak by mutableStateOf(prefs.getBoolean("speak", false))
    var provId by mutableStateOf(prefs.getString("prov", "groq") ?: "groq")
    var estate by mutableStateOf(EState.IDLE)
    var level by mutableFloatStateOf(0f)
    var status by mutableStateOf("")
    var busy by mutableStateOf(false)
    var keyTick by mutableIntStateOf(0)
    var voice: Voice? = null

    val prov: Prov get() = PROVS.first { it.id == provId }
    val current: Chat? get() = chats.firstOrNull { it.id == curId }
    fun hasKey(id: String = provId) = !secure.get(id).isNullOrBlank()
    fun model(id: String = provId) = prefs.getString("model_$id", null) ?: PROVS.first { it.id == id }.defModel

    init { load() }

    private fun load() {
        try {
            if (chatFile.exists()) {
                val a = JSONArray(chatFile.readText())
                chats = (0 until a.length()).map { i ->
                    val o = a.getJSONObject(i); val m = o.getJSONArray("m")
                    Chat(o.getString("id"), o.getString("t"), (0 until m.length()).map { j ->
                        val x = m.getJSONObject(j); Msg(x.getString("r"), x.getString("x")) })
                }
                curId = chats.firstOrNull()?.id ?: ""
            }
            if (memFile.exists()) { val a = JSONArray(memFile.readText()); memories = (0 until a.length()).map { a.getString(it) } }
        } catch (e: Exception) { status = "Could not read saved data: ${e.message}" }
    }
    @Synchronized fun save() {
        try {
            val a = JSONArray()
            chats.forEach { c -> a.put(JSONObject().put("id", c.id).put("t", c.title).put("m", JSONArray().also { m ->
                c.msgs.forEach { m.put(JSONObject().put("r", it.role).put("x", it.text)) } })) }
            chatFile.writeText(a.toString())
            memFile.writeText(JSONArray(memories).toString())
        } catch (e: Exception) { status = "Save failed: ${e.message}" }
    }

    fun setProv(id: String) { provId = id; prefs.edit().putString("prov", id).apply() }
    fun setModel(id: String, m: String) { prefs.edit().putString("model_$id", m.trim()).apply(); keyTick++ }
    fun setSpeak(v: Boolean) { speak = v; prefs.edit().putBoolean("speak", v).apply() }
    fun setMemoryOn(v: Boolean) { memoryOn = v; prefs.edit().putBoolean("memoryOn", v).apply() }
    fun saveKey(id: String, k: String) { secure.put(id, k.trim()); keyTick++ }
    fun removeKey(id: String) { secure.remove(id); keyTick++ }

    fun newChat() { val c = Chat(UUID.randomUUID().toString(), "New chat", emptyList()); chats = listOf(c) + chats; curId = c.id; save() }
    fun open(id: String) { curId = id }
    fun delete(id: String) { chats = chats.filter { it.id != id }; if (curId == id) curId = chats.firstOrNull()?.id ?: ""; save() }
    fun addMemory(t: String) { if (t.isNotBlank()) { memories = memories + t.trim(); save() } }
    fun delMemory(i: Int) { memories = memories.filterIndexed { n, _ -> n != i }; save() }
    fun wipeAll() {
        chats = emptyList(); curId = ""; memories = emptyList(); secure.clear()
        prefs.edit().clear().apply(); chatFile.delete(); memFile.delete(); keyTick++
    }
    private fun update(id: String, f: (Chat) -> Chat) { chats = chats.map { if (it.id == id) f(it) else it } }

    fun stop() { Llm.cancelled = true; voice?.stopSpeaking() }

    fun send(raw: String) {
        val text = raw.trim()
        if (text.isEmpty() || busy) return
        val p = prov
        val key = secure.get(p.id)
        if (key.isNullOrBlank()) { status = "No API key for ${p.label}. Add one in Settings."; estate = EState.ERROR; return }
        if (memoryOn && text.startsWith("remember ", true)) addMemory(text.substring(9))
        if (current == null) newChat()
        val id = curId
        update(id) { it.copy(title = if (it.msgs.isEmpty()) text.take(40) else it.title, msgs = it.msgs + Msg("user", text) + Msg("assistant", "")) }
        val history = (current?.msgs ?: emptyList()).dropLast(1).takeLast(20).map { it.role to it.text }
        val sys = "You are Zaeem, a concise, helpful, privacy-respecting personal assistant by Zaeem Technologies." +
            if (memoryOn && memories.isNotEmpty()) " Facts the user asked you to remember: " + memories.joinToString("; ") + "." else ""
        val model = model(p.id)
        busy = true; estate = EState.THINKING; status = ""; Llm.cancelled = false
        Thread {
            val sb = StringBuilder()
            try {
                Llm.stream(p, key, model, listOf("system" to sys) + history) { d ->
                    sb.append(d); val s = sb.toString()
                    update(id) { it.copy(msgs = it.msgs.dropLast(1) + Msg("assistant", s)) }
                }
                if (sb.isEmpty() && !Llm.cancelled) throw LlmError("The provider returned an empty response.")
                estate = EState.IDLE
                if (speak && sb.isNotEmpty() && !Llm.cancelled) voice?.speak(sb.toString())
            } catch (e: LlmError) {
                status = e.message ?: "Error"; estate = EState.ERROR
                if (sb.isEmpty()) update(id) { it.copy(msgs = it.msgs.dropLast(1) + Msg("assistant", "⚠ " + status)) }
            } catch (e: Exception) {
                status = "Unexpected error: ${e.message}"; estate = EState.ERROR
                if (sb.isEmpty()) update(id) { it.copy(msgs = it.msgs.dropLast(1) + Msg("assistant", "⚠ " + status)) }
            } finally { busy = false; save() }
        }.start()
    }
}
