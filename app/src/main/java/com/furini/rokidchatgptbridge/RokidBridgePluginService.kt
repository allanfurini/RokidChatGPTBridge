package com.furini.rokidchatgptbridge

import android.content.Intent
import android.view.KeyEvent
import com.anezium.rokidbus.client.plugin.NexusCard
import com.anezium.rokidbus.client.plugin.NexusPluginService
import com.anezium.rokidbus.client.plugin.NexusSdkResult
import com.anezium.rokidbus.client.plugin.NexusSpeechCallbacks
import com.anezium.rokidbus.client.plugin.NexusSpeechError
import com.anezium.rokidbus.client.plugin.NexusSpeechSession
import com.anezium.rokidbus.client.plugin.NexusSpeechState
import com.anezium.rokidbus.client.plugin.NexusSpeechStopReason
import com.anezium.rokidbus.client.plugin.NexusSurfaceSession
import com.anezium.rokidbus.client.plugin.NexusTtsCallbacks
import com.anezium.rokidbus.client.plugin.NexusTtsDoneReason
import com.anezium.rokidbus.client.plugin.NexusTtsSession
import com.anezium.rokidbus.shared.plugin.NexusInputEvent

class RokidBridgePluginService : NexusPluginService() {

    private var surface: NexusSurfaceSession? = null
    private var speech: NexusSpeechSession? = null
    private var tts: NexusTtsSession? = null

    private val speechCallbacks = object : NexusSpeechCallbacks {
        override fun onSpeechStarted(realtime: Boolean) {
            show(
                title = "Ouvindo…",
                body = "Fale normalmente.",
                footer = "microfone do Rokid"
            )
        }

        override fun onSpeechState(state: NexusSpeechState) = Unit

        override fun onSpeechPartial(text: String) {
            if (text.isNotBlank()) {
                show(
                    title = "Você",
                    body = text,
                    footer = "ouvindo…"
                )
            }
        }

        override fun onSpeechFinal(text: String) {
            if (text.isBlank()) return
            show(
                title = "Você",
                body = text,
                footer = "enviando ao ChatGPT…"
            )
            sendToOfficialChatGpt(text)
        }

        override fun onSpeechStopped(
            reason: NexusSpeechStopReason,
            error: NexusSpeechError?,
        ) {
            speech = null
            if (error != null) {
                show(
                    title = "Voz",
                    body = "Não consegui reconhecer a fala.",
                    footer = "toque para tentar novamente"
                )
            }
        }
    }

    private val ttsCallbacks = object : NexusTtsCallbacks {
        override fun onTtsStarted(utteranceId: String) = Unit

        override fun onTtsDone(
            utteranceId: String,
            reason: NexusTtsDoneReason,
        ) = Unit
    }

    override fun onNexusOpen() {
        BridgeState.plugin = this

        surface?.hide()
        surface = nexusSurfaceSession(SURFACE_ID)

        show(
            title = "ChatGPT",
            body = "Toque no centro ou use o botão de IA e fale.",
            footer = "Rokid → celular → ChatGPT"
        )

        if (tts == null) {
            tts = nexusTtsSession(ttsCallbacks)
        }
    }

    override fun onNexusClose() {
        speech?.stop()
        speech = null

        tts?.close()
        tts = null

        surface?.hide()
        surface = null

        if (BridgeState.plugin === this) {
            BridgeState.plugin = null
        }
    }

    override fun onNexusGlassesAiButton(active: Boolean) {
        if (active) {
            beginSpeech()
        }
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

        val newSpeech = nexusSpeechSession(speechCallbacks)
        if (newSpeech == null) {
            show(
                title = "Voz",
                body = "Speech-to-text indisponível.",
                footer = "verifique o acesso do plugin no Nexus"
            )
            return
        }

        speech = newSpeech

        when (newSpeech.start(language = "pt")) {
            NexusSdkResult.SENT -> Unit

            NexusSdkResult.CAPABILITY_NOT_GRANTED -> {
                speech = null
                show(
                    title = "Permissão",
                    body = "Libere Speech to text para este plugin no Rokid Nexus.",
                    footer = "Nexus → Settings → Plugin access"
                )
            }

            else -> {
                speech = null
                show(
                    title = "Voz",
                    body = "Não consegui iniciar a escuta.",
                    footer = "toque para tentar novamente"
                )
            }
        }
    }

    private fun sendToOfficialChatGpt(text: String) {
        BridgeState.pendingPrompt = text

        val launch = packageManager.getLaunchIntentForPackage(CHATGPT_PACKAGE)
        if (launch == null) {
            show(
                title = "ChatGPT",
                body = "O aplicativo oficial do ChatGPT não foi encontrado no celular.",
                footer = "instale o ChatGPT oficial"
            )
            return
        }

        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(launch)

        show(
            title = "ChatGPT",
            body = "Abrindo sua conversa no celular…",
            footer = "aguarde"
        )
    }

    fun onChatGptVisibleText(text: String) {
        val answer = extractLikelyAssistantAnswer(text)
        if (answer.isBlank()) return

        show(
            title = "ChatGPT",
            body = answer,
            footer = "toque para falar novamente"
        )

        val session = tts ?: nexusTtsSession(ttsCallbacks)?.also { tts = it }
        session?.speak(answer.take(900))
    }

    private fun extractLikelyAssistantAnswer(full: String): String {
        val lines = full.lines()
            .map { it.trim() }
            .filter { it.length > 1 }

        if (lines.isEmpty()) return ""

        return lines
            .takeLast(12)
            .joinToString("\n")
            .take(1000)
    }

    private fun show(title: String, body: String, footer: String) {
        val card = NexusCard(
            title = title.take(120),
            lines = body
                .replace('\n', ' ')
                .chunked(220)
                .take(8),
            footer = footer.take(240),
            contentKey = "chatgpt-bridge",
            handlesBack = true,
        )

        val result = surface?.showCard(card)
        if (result != NexusSdkResult.SENT) {
            surface?.updateCard(card)
        }
    }

    private companion object {
        const val SURFACE_ID = "main"
        const val CHATGPT_PACKAGE = "com.openai.chatgpt"
    }
}
