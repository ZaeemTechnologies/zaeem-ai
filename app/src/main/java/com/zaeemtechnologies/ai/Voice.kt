package com.zaeemtechnologies.ai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/** Uses the platform speech services. Availability depends on the device (often missing on GrapheneOS without a speech service). */
class Voice(private val ctx: Context, private val st: AppState) {
    private var sr: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ready = false

    init {
        tts = TextToSpeech(ctx) { s ->
            ready = s == TextToSpeech.SUCCESS
            if (ready) {
                tts?.language = Locale.getDefault()
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String?) { st.estate = EState.SPEAKING }
                    override fun onDone(id: String?) { if (st.estate == EState.SPEAKING) st.estate = EState.IDLE }
                    @Deprecated("Deprecated in Java") override fun onError(id: String?) { st.estate = EState.ERROR; st.status = "Speech output failed." }
                })
            } else st.status = "No text-to-speech engine available on this device."
        }
    }

    fun speak(text: String) {
        if (!ready) { st.status = "Text-to-speech is not available."; return }
        val clean = text.replace(Regex("[*_`#>]"), "")
        tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "zaeem")
    }
    fun stopSpeaking() { tts?.stop(); if (st.estate == EState.SPEAKING) st.estate = EState.IDLE }

    fun listen(onText: (String) -> Unit) {
        if (!SpeechRecognizer.isRecognitionAvailable(ctx)) {
            st.status = "No speech recognition service on this device (common on GrapheneOS without Google). Type instead."
            st.estate = EState.ERROR; return
        }
        stopSpeaking(); sr?.destroy()
        val r = SpeechRecognizer.createSpeechRecognizer(ctx); sr = r
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rms: Float) { st.level = ((rms + 2f) / 12f).coerceIn(0f, 1f) }
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() { st.level = 0f }
            override fun onError(e: Int) { st.level = 0f; st.estate = EState.ERROR; st.status = "Speech recognition error (code $e)." }
            override fun onResults(b: Bundle?) {
                st.level = 0f
                val t = b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (t.isNullOrBlank()) st.estate = EState.IDLE else { st.estate = EState.IDLE; onText(t) }
            }
            override fun onPartialResults(b: Bundle?) {}
            override fun onEvent(t: Int, b: Bundle?) {}
        })
        st.estate = EState.LISTENING; st.status = ""
        r.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM))
    }
    fun cancelListen() { sr?.cancel(); st.level = 0f; if (st.estate == EState.LISTENING) st.estate = EState.IDLE }
    fun release() { sr?.destroy(); tts?.shutdown() }
}
