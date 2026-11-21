package com.zaeemtechnologies.ai

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.collect
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL

/** On-device inference (LiteRT-LM). Model is a pinned, Apache-2.0 Qwen3-1.7B int4 file; nothing is sent anywhere during chat. */
object LocalLlm {
    const val NAME = "Qwen3 1.7B (int4, LiteRT-LM)"
    const val URL_ = "https://huggingface.co/litert-community/Qwen3-1.7B/resolve/main/Qwen3-1.7B_dynamic_wi4b32_afp32.litertlm"
    const val SIZE = 977_184_032L
    const val LICENSE = "Apache-2.0 (Qwen team / Google LiteRT community)"

    private lateinit var dir: File
    private lateinit var cache: String
    private var engine: Engine? = null

    var dlBytes by mutableStateOf(0L)
    var dlActive by mutableStateOf(false)
    var msg by mutableStateOf("")
    var tick by mutableStateOf(0)
    @Volatile private var cancel = false

    fun init(ctx: Context) { dir = File(ctx.filesDir, "models").also { it.mkdirs() }; cache = ctx.cacheDir.path; tick++ }
    private val file get() = File(dir, "model.litertlm")
    val installed: Boolean get() { tick; return ::dir.isInitialized && file.exists() && file.length() > 100_000_000L }
    val sizeOnDisk: Long get() { tick; return if (::dir.isInitialized && file.exists()) file.length() else 0L }

    fun download() {
        if (dlActive) return
        dlActive = true; cancel = false; msg = "Starting download…"
        Thread {
            val part = File(dir, "model.part")
            try {
                var have = if (part.exists()) part.length() else 0L
                if (have > SIZE) { part.delete(); have = 0 }
                if (have < SIZE) {
                    val c = URL(URL_).openConnection() as HttpURLConnection
                    c.connectTimeout = 20000; c.readTimeout = 30000
                    c.setRequestProperty("User-Agent", "ZaeemAI/0.3 (Android)")
                    if (have > 0) c.setRequestProperty("Range", "bytes=$have-")
                    val code = c.responseCode
                    if (code != 200 && code != 206) throw Exception("Download failed (HTTP $code).")
                    if (code == 200 && have > 0) { part.delete(); have = 0 }
                    RandomAccessFile(part, "rw").use { raf ->
                        raf.seek(have)
                        c.inputStream.use { ins ->
                            val buf = ByteArray(256 * 1024); dlBytes = have
                            while (true) {
                                if (cancel) { msg = "Paused. Tap Download to resume."; return@Thread }
                                val n = ins.read(buf); if (n < 0) break
                                raf.write(buf, 0, n); dlBytes += n
                            }
                        }
                    }
                }
                if (part.length() != SIZE) throw Exception("Size check failed (${part.length()} of $SIZE bytes). Tap Download to retry.")
                file.delete(); part.renameTo(file); msg = "Model installed."; tick++
            } catch (e: Exception) { msg = "Download error: ${e.message ?: e.javaClass.simpleName}" }
            finally { dlActive = false }
        }.start()
    }
    fun pause() { cancel = true }

    fun importFrom(ctx: Context, uri: Uri) {
        if (dlActive) return
        dlActive = true; msg = "Importing…"
        Thread {
            try {
                val tmp = File(dir, "import.part")
                ctx.contentResolver.openInputStream(uri)!!.use { i -> tmp.outputStream().use { o -> i.copyTo(o) } }
                if (tmp.length() < 100_000_000L) { tmp.delete(); throw Exception("That file is too small to be a model.") }
                unload(); file.delete(); tmp.renameTo(file); msg = "Model imported."; tick++
            } catch (e: Exception) { msg = "Import error: ${e.message}" } finally { dlActive = false }
        }.start()
    }

    fun delete() { unload(); file.delete(); File(dir, "model.part").delete(); dlBytes = 0; msg = "Model deleted."; tick++ }
    @Synchronized fun unload() { try { engine?.close() } catch (_: Throwable) {}; engine = null }

    @Synchronized private fun eng(): Engine {
        engine?.let { return it }
        val e = Engine(EngineConfig(modelPath = file.path, backend = Backend.CPU(), cacheDir = cache))
        e.initialize(); engine = e; return e
    }

    /** Blocking; call from a worker thread. */
    fun chat(system: String, history: List<Pair<String, String>>, user: String, onDelta: (String) -> Unit) {
        if (!installed) throw LlmError("No on-device model installed. Open Settings → On-device and download or import one.")
        val sys = StringBuilder(system)
        if (history.isNotEmpty()) {
            sys.append("\n\nConversation so far:\n")
            history.forEach { sys.append(if (it.first == "user") "User: " else "Assistant: ").append(it.second).append('\n') }
        }
        try {
            val conv = eng().createConversation(ConversationConfig(systemInstruction = Contents.of(sys.toString())))
            conv.use {
                runBlocking {
                    it.sendMessageAsync(user).collect { part ->
                        if (Llm.cancelled) throw InterruptedException()
                        onDelta(part.toString())
                    }
                }
            }
        } catch (e: InterruptedException) { /* stopped by user */ }
        catch (e: OutOfMemoryError) { unload(); throw LlmError("Low memory — close other apps and try again.") }
        catch (e: LlmError) { throw e }
        catch (e: Throwable) { throw LlmError("On-device model error: ${e.message ?: e.javaClass.simpleName}") }
    }
}
