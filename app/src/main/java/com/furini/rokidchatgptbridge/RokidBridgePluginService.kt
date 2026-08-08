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
            DiagnosticStore.add(this@RokidBridgePluginService, "Microfone/STT iniciado.")
            show("Ouvindo…", "Fale normalmente.", "microfone do Rokid")
        }

        override fun onSpeechState(state: NexusSpeechState) {
            DiagnosticStore.add(this@RokidBridgePluginService, "STT: $state")
        }

        override fun onSpeechPartial(text: String) {
            if (text.isNotBlank()) {
                show("Você", text, "ouvindo…")
            }
        }

        override fun onSpeechFinal(text: String) {
            if (text.isBlank()) return
            DiagnosticStore.add(this@RokidBridgePluginService, "Fala final recebida do Rokid.")
            show("Você", text, "enviando ao ChatGPT…")
            sendToOfficialChatGpt(text)
        }

        override fun onSpeechStopped(
            reason: NexusSpeechStopReason,
            error: NexusSpeechError?,
        ) {
            speech = null
            DiagnosticStore.add(
                this@RokidBridgePluginService,
                "STT encerrado: $reason" + if (error != null) " (erro)" else ""
            )
            if (error != null) {
                show("Voz", "Não consegui reconhecer a fala.", "tente novamente")
            }
        }
    }

    private val ttsCallbacks = object : NexusTtsCallbacks {
        override fun onTtsStarted(utteranceId: String) {
            DiagnosticStore.add(this@RokidBridgePluginService, "TTS iniciado.")
        }

        override fun onTtsDone(
            utteranceId: String,
            reason: NexusTtsDoneReason,
        ) {
            DiagnosticStore.add(this@RokidBridgePluginService, "TTS encerrado: $reason")
        }
    }

    override fun onNexusOpen() {
        BridgeState.plugin = this
        DiagnosticStore.add(this, "Plugin Nexus aberto.")

        surface?.hide()
        surface = nexusSurfaceSession(SURFACE_ID)

        show(
            title = "ChatGPT Bridge",
            body = "Conectado. Toque no centro ou use o botão de IA e fale.",
            footer = "v0.5 diagnóstico"
        )

        if (tts == null) {
            tts = nexusTtsSession(ttsCallbacks)
        }
    }

    override fun onNexusClose() {
        DiagnosticStore.add(this, "Plugin Nexus fechado.")

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
            DiagnosticStore.add(this, "Botão AI do Rokid acionado.")
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

    fun testHudFromPhone(): Boolean {
        if (surface == null) return false
        DiagnosticStore.add(this, "Teste manual do HUD enviado.")
        show(
            "TESTE HUD",
            "Se você está lendo isto nos óculos, o caminho celular → Nexus → Rokid está funcionando.",
            "Rokid ChatGPT Bridge v0.5"
        )
        return true
    }

    fun startSpeechFromPhone(): Boolean {
        if (surface == null) return false
        DiagnosticStore.add(this, "Teste manual de microfone solicitado.")
        beginSpeech()
        return true
    }

    fun testTtsFromPhone(): Boolean {
        if (surface == null) return false
        DiagnosticStore.add(this, "Teste manual de TTS solicitado.")
        val session = tts ?: nexusTtsSession(ttsCallbacks)?.also { tts = it }
        session?.speak("Teste de áudio do Rokid ChatGPT Bridge.")
        return session != null
    }

    private fun beginSpeech() {
        if (speech != null) {
            DiagnosticStore.add(this, "STT já estava ativo.")
            return
        }

        val newSpeech = nexusSpeechSession(speechCallbacks)
        if (newSpeech == null) {
            DiagnosticStore.add(this, "Falha ao criar sessão STT.")
            show("Voz", "Speech-to-text indisponível.", "verifique permissões Nexus")
            return
        }

        speech = newSpeech

        when (newSpeech.start(language = "pt")) {
            NexusSdkResult.SENT -> DiagnosticStore.add(this, "Pedido STT enviado ao Nexus.")

            NexusSdkResult.CAPABILITY_NOT_GRANTED -> {
                speech = null
                DiagnosticStore.add(this, "Permissão STT não concedida.")
                show("Permissão", "Libere Speech to text no Nexus.", "Plugin access")
            }

            NexusSdkResult.NOT_REGISTERED -> {
                speech = null
                DiagnosticStore.add(this, "Plugin ainda não registrado/aprovado no Nexus.")
                show("Nexus", "Plugin ainda não aprovado.", "Plugin access")
            }

            else -> {
                speech = null
                DiagnosticStore.add(this, "Nexus recusou início do STT.")
                show("Voz", "Não consegui iniciar a escuta.", "tente novamente")
            }
        }
    }

    private fun sendToOfficialChatGpt(text: String) {
        BridgeState.pendingPrompt = text
        DiagnosticStore.add(this, "Pergunta preparada para o ChatGPT.")

        val launch = packageManager.getLaunchIntentForPackage(CHATGPT_PACKAGE)
        if (launch == null) {
            DiagnosticStore.add(this, "ChatGPT oficial não encontrado.")
            show("ChatGPT", "App oficial não encontrado.", "instale no celular")
            return
        }

        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(launch)

        show(
            "ChatGPT",
            "Abrindo a conversa no celular…",
            "aguarde"
        )
    }

    fun onChatGptVisibleText(text: String) {
        val answer = extractLikelyAssistantAnswer(text)
        if (answer.isBlank()) return

        DiagnosticStore.add(this, "Texto visível do ChatGPT recebido.")
        show("ChatGPT", answer, "toque para falar novamente")

        val session = tts ?: nexusTtsSession(ttsCallbacks)?.also { tts = it }
        session?.speak(answer.take(900))
    }

    private fun extractLikelyAssistantAnswer(full: String): String {
        val lines = full.lines()
            .map { it.trim() }
            .filter { it.length > 1 }

        if (lines.isEmpty()) return ""

        return lines.takeLast(12)
            .joinToString("\n")
            .take(1000)
    }

    private fun show(title: String, body: String, footer: String) {
        val card = NexusCard(
            title = title.take(120),
            lines = body.replace('\n', ' ').chunked(220).take(8),
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
