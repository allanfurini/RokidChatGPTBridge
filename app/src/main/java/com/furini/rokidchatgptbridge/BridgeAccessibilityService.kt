package com.furini.rokidchatgptbridge

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class BridgeAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        DiagnosticStore.add(this, "Serviço de acessibilidade conectado.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.packageName?.toString() != CHATGPT_PACKAGE) return
        val root = rootInActiveWindow ?: return

        BridgeState.pendingPrompt?.let { prompt ->
            if (injectPrompt(root, prompt)) {
                BridgeState.pendingPrompt = null
                DiagnosticStore.add(this, "Texto inserido no ChatGPT.")
            }
        }

        val visible = mutableListOf<String>()
        collectText(root, visible)
        val normalized = normalize(visible)

        if (normalized.isNotBlank() && normalized != BridgeState.latestVisibleText) {
            BridgeState.latestVisibleText = normalized
            DiagnosticStore.add(this, "Mudança de texto detectada no ChatGPT.")
            BridgeState.cxrManager?.showText(
                "ChatGPT",
                normalized.lines().takeLast(12).joinToString("\n").take(1200)
            )
        }
    }

    override fun onInterrupt() {
        DiagnosticStore.add(this, "Acessibilidade interrompida.")
    }

    private fun injectPrompt(root: AccessibilityNodeInfo, prompt: String): Boolean {
        val editable = findEditable(root) ?: run {
            DiagnosticStore.add(this, "Caixa de texto do ChatGPT não encontrada.")
            return false
        }

        val args = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                prompt
            )
        }

        val set = editable.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        if (!set) {
            DiagnosticStore.add(this, "Falha ao inserir texto na caixa do ChatGPT.")
            return false
        }

        val send = findNode(root) { node ->
            if (!node.isClickable) return@findNode false
            val label = (
                (node.contentDescription?.toString() ?: "") + " " +
                (node.text?.toString() ?: "")
            ).lowercase()

            label.contains("send") ||
            label.contains("enviar") ||
            label.contains("submit")
        }

        if (send != null) {
            val clicked = send.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            DiagnosticStore.add(
                this,
                if (clicked) "Botão Enviar acionado." else "Botão Enviar encontrado, mas clique falhou."
            )
            return true
        }

        DiagnosticStore.add(this, "Texto inserido, mas botão Enviar não foi identificado.")
        editable.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        return true
    }

    private fun findEditable(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isEditable && node.isVisibleToUser) return node
        for (i in 0 until node.childCount) {
            val found = findEditable(node.getChild(i))
            if (found != null) return found
        }
        return null
    }

    private fun findNode(
        node: AccessibilityNodeInfo?,
        predicate: (AccessibilityNodeInfo) -> Boolean
    ): AccessibilityNodeInfo? {
        if (node == null) return null
        if (predicate(node)) return node
        for (i in 0 until node.childCount) {
            val found = findNode(node.getChild(i), predicate)
            if (found != null) return found
        }
        return null
    }

    private fun collectText(node: AccessibilityNodeInfo?, out: MutableList<String>) {
        if (node == null) return
        if (node.isVisibleToUser) {
            node.text?.toString()?.trim()?.takeIf { it.length > 1 }?.let(out::add)
            node.contentDescription?.toString()?.trim()?.takeIf { it.length > 1 }?.let(out::add)
        }
        for (i in 0 until node.childCount) collectText(node.getChild(i), out)
    }

    private fun normalize(items: List<String>): String {
        val ignored = listOf(
            "chatgpt", "novo chat", "new chat", "enviar", "send",
            "copiar", "copy", "parar", "stop", "voz", "voice"
        )

        return items
            .map { it.replace(Regex("\\s+"), " ").trim() }
            .filter { value ->
                val low = value.lowercase()
                ignored.none { low == it }
            }
            .distinct()
            .joinToString("\n")
            .takeLast(5000)
    }

    companion object {
        private const val CHATGPT_PACKAGE = "com.openai.chatgpt"
    }
}
