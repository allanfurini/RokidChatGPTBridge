package com.furini.rokidchatgptbridge

import android.content.Intent
import android.view.KeyEvent
import com.anezium.rokidbus.sdk.NexusCard
import com.anezium.rokidbus.sdk.NexusInputEvent
import com.anezium.rokidbus.sdk.NexusPluginService
import com.anezium.rokidbus.sdk.NexusSdkResult
import com.anezium.rokidbus.sdk.NexusSpeechCallbacks
import com.anezium.rokidbus.sdk.NexusSpeechError
import com.anezium.rokidbus.sdk.NexusSpeechSession
import com.anezium.rokidbus.sdk.NexusSpeechState
import com.anezium.rokidbus.sdk.NexusSpeechStopReason
import com.anezium.rokidbus.sdk.NexusSurfaceSession
import com.anezium.rokidbus.sdk.NexusTtsCallbacks
import com.anezium.rokidbus.sdk.NexusTtsDoneReason
import com.anezium.rokidbus.sdk.NexusTtsSession

class RokidBridgePluginService : NexusPluginService() {

    private var surface: NexusSurfaceSession? = null
    private var speech: NexusSpeechSession? = null
    private var tts: NexusTtsSession? = null

    override fun onNexusOpen() {
        BridgeState.plugin = this
        surface = nexusSurfaceSession("chat")
        show(
            title = "ChatGPT",
            body = "Toque ou segure o botão de IA e fale.",
            footer = "fala → celular → ChatGPT"
        )
        initTts()
    }

    override fun onNexusClose() {
        speech?.stop()
        speech = null
        tts?.close()
        tts = null
        surface?.hide()
        surface = null
        if (BridgeState.plugin === this) BridgeState.plugin = null
    }

    override fun onNexusGlassesAiButton(active: Boolean) {
        if (active) beginSpeech()
    }

    override fun onNexusInput(event: NexusInputEvent) {
        if (event.action != KeyEvent.ACTION_DOWN) return

        when (event.keyCode) {
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_DPAD_CENTER -> beginSpeech()

            KeyEvent.KEYCODE_BACK -> surface?.hide()
        }
    }

    private fun beginSpeech() {
        if (speech != null) return

        val session = nexusSpeechSession(object : NexusSpeechCallbacks {
            override fun onSpeechStarted(realtime: Boolean) {
                show("Ouvindo…", "Fale normalmente.", "microfone do Rokid")
            }

            override fun onSpeechState(state: NexusSpeechState) {
                // O HUD é atualizado pelas parciais/final.
            }

            override fun onSpeechPartial(text: String) {
                if (text.isNotBlank()) show("Você", text, "ouvindo…")
            }

            override fun onSpeechFinal(text: String) {
                if (text.isBlank()) return
                show("Você", text, "enviando ao ChatGPT…")
                sendToOfficialChatGpt(text)
            }

            override fun onSpeechStopped(
                reason: NexusSpeechStopReason,
                error: NexusSpeechError?
            ) {
                speech = null
                if (error != null) {
                    show("Voz", "Falha ao reconhecer a fala.", "tente novamente")
                }
            }
        }) ?: run {
            show("Voz", "Speech-to-text indisponível.", "verifique permissões Nexus")
            return
        }

        speech = session
        when (session.start(language = "pt")) {
            NexusSdkResult.SENT -> Unit
            NexusSdkResult.CAPABILITY_NOT_GRANTED -> {
                speech = null
                show("Permissão", "Libere Speech-to-text no Nexus.", "Plugin access")
            }
            else -> {
                speech = null
                show("Voz", "Não consegui iniciar a escuta.", "tente novamente")
            }
        }
    }

    private fun sendToOfficialChatGpt(text: String) {
        BridgeState.pendingPrompt = text

        val launch = packageManager.getLaunchIntentForPackage("com.openai.chatgpt")
        if (launch == null) {
            show("ChatGPT", "App oficial não encontrado no celular.", "instale o ChatGPT")
            return
        }

        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(launch)

        show(
            "ChatGPT",
            "Abrindo a conversa do celular e enviando sua pergunta…",
            "aguarde"
        )
    }

    fun onChatGptVisibleText(text: String) {
        val last = extractLikelyAssistantAnswer(text)
        if (last.isBlank()) return

        show(
            title = "ChatGPT",
            body = last,
            footer = "toque para falar novamente"
        )

        // Leitura opcional. O Nexus roteia para os óculos/earbuds.
        tts?.speak(last.take(900))
    }

    private fun extractLikelyAssistantAnswer(full: String): String {
        val lines = full.lines()
            .map { it.trim() }
            .filter { it.length > 1 }

        if (lines.isEmpty()) return ""

        // Heurística inicial: devolve o final da árvore visível, onde normalmente
        // está a resposta mais recente do ChatGPT.
        return lines.takeLast(12).joinToString("\n").take(1000)
    }

    private fun show(title: String, body: String, footer: String) {
        val lines = body
            .chunked(110)
            .take(8)

        surface?.showCard(
            NexusCard(
                title = title.take(40),
                lines = lines,
                footer = footer.take(60)
            )
        )
    }

    private fun initTts() {
        tts = nexusTtsSession(object : NexusTtsCallbacks {
            override fun onTtsStarted(utteranceId: String) = Unit
            override fun onTtsDone(
                utteranceId: String,
                reason: NexusTtsDoneReason
            ) = Unit
        })
    }
}
