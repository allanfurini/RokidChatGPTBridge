package com.furini.rokidchatgptbridge

import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.io.FileOutputStream

class CxrSpeechRecognizer(
    private val context: Context,
    private val cxr: HiRokidCxrManager,
    private val onFinal: (String) -> Unit
) : HiRokidCxrManager.AudioListener {

    private var recognizer: SpeechRecognizer? = null
    private var readFd: ParcelFileDescriptor? = null
    private var writeFd: ParcelFileDescriptor? = null
    private var output: FileOutputStream? = null
    private var bytesReceived = 0L

    fun start(): Boolean {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            DiagnosticStore.add(context, "Reconhecimento de fala Android indisponível.")
            return false
        }

        stop()

        val pipe = ParcelFileDescriptor.createPipe()
        readFd = pipe[0]
        writeFd = pipe[1]
        output = FileOutputStream(writeFd!!.fileDescriptor)
        bytesReceived = 0

        val sr = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = sr

        sr.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                DiagnosticStore.add(context, "Android STT pronto; aguardando PCM do Rokid.")
            }

            override fun onBeginningOfSpeech() {
                DiagnosticStore.add(context, "Android STT detectou fala.")
            }

            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() {
                DiagnosticStore.add(context, "Fim da fala detectado.")
            }

            override fun onError(error: Int) {
                DiagnosticStore.add(context, "Erro Android STT: $error; PCM=$bytesReceived bytes.")
                cxr.stopAudio()
            }

            override fun onResults(results: Bundle?) {
                val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = list?.firstOrNull().orEmpty().trim()
                DiagnosticStore.add(context, "STT final: ${text.length} caracteres; PCM=$bytesReceived bytes.")
                cxr.stopAudio()
                if (text.isNotBlank()) {
                    BridgeState.lastSpeechText = text
                    onFinal(text)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val list = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = list?.firstOrNull().orEmpty()
                if (text.isNotBlank()) BridgeState.lastSpeechText = text
            }

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)

            // Android 13+ injected-audio route.
            putExtra("android.speech.extra.AUDIO_SOURCE", readFd)
            putExtra("android.speech.extra.AUDIO_SOURCE_CHANNEL_COUNT", 1)
            putExtra(
                "android.speech.extra.AUDIO_SOURCE_ENCODING",
                AudioFormat.ENCODING_PCM_16BIT
            )
            putExtra("android.speech.extra.AUDIO_SOURCE_SAMPLING_RATE", 16000)
            putExtra("android.speech.extra.SEGMENTED_SESSION", "android.speech.extra.AUDIO_SOURCE")
        }

        cxr.setAudioListener(this)
        sr.startListening(intent)

        val ok = cxr.startAudio()
        if (!ok) {
            DiagnosticStore.add(context, "CXR não iniciou o microfone dos óculos.")
            stop()
            return false
        }

        return true
    }

    fun stop() {
        try { cxr.stopAudio() } catch (_: Throwable) {}
        try { recognizer?.cancel() } catch (_: Throwable) {}
        try { recognizer?.destroy() } catch (_: Throwable) {}
        recognizer = null
        try { output?.close() } catch (_: Throwable) {}
        output = null
        try { readFd?.close() } catch (_: Throwable) {}
        try { writeFd?.close() } catch (_: Throwable) {}
        readFd = null
        writeFd = null
    }

    override fun onAudio(data: ByteArray) {
        try {
            output?.write(data)
            output?.flush()
            bytesReceived += data.size
            if (bytesReceived == data.size.toLong()) {
                DiagnosticStore.add(context, "Primeiro PCM recebido do microfone Rokid.")
            }
        } catch (t: Throwable) {
            DiagnosticStore.add(context, "Falha ao injetar PCM no Android STT: ${t.message}")
        }
    }

    override fun onAudioState(started: Boolean) {
        DiagnosticStore.add(context, "Estado áudio CXR: $started")
    }

    override fun onAudioError(code: Int, info: String?) {
        DiagnosticStore.add(context, "Erro áudio CXR $code: ${info.orEmpty()}")
    }
}
