package com.furini.rokidchatgptbridge

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class SetupActivity : Activity() {

    private lateinit var statusText: TextView
    private lateinit var logText: TextView
    private lateinit var promptText: EditText
    private val handler = Handler(Looper.getMainLooper())

    private val refresher = object : Runnable {
        override fun run() {
            refreshStatus()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 38, 28, 40)
            setBackgroundColor(Color.BLACK)
        }

        content.addView(TextView(this).apply {
            text = "Rokid ChatGPT Bridge"
            textSize = 25f
            setTextColor(Color.WHITE)
        })

        content.addView(TextView(this).apply {
            text = "v0.5 • painel de diagnóstico • sem API OpenAI"
            textSize = 15f
            setTextColor(Color.LTGRAY)
            setPadding(0, 8, 0, 20)
        })

        statusText = TextView(this).apply {
            textSize = 16f
            setTextColor(Color.WHITE)
            setPadding(0, 6, 0, 16)
        }
        content.addView(statusText)

        content.addView(button("1. Ativar acessibilidade") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })

        content.addView(button("2. Abrir Rokid Nexus") {
            openPackage("com.anezium.rokidbus.phone", "Rokid Nexus não encontrado.")
        })

        content.addView(button("3. Abrir ChatGPT oficial") {
            openPackage("com.openai.chatgpt", "ChatGPT oficial não encontrado.")
        })

        content.addView(button("4. Testar HUD agora") {
            val ok = BridgeState.plugin?.testHudFromPhone() == true
            if (!ok) {
                toast("Abra primeiro o plugin ChatGPT Bridge no Rokid Nexus/óculos.")
            }
        })

        content.addView(button("5. Testar microfone do Rokid") {
            val ok = BridgeState.plugin?.startSpeechFromPhone() == true
            if (!ok) {
                toast("Abra primeiro o plugin ChatGPT Bridge no Rokid Nexus/óculos.")
            }
        })

        content.addView(button("6. Testar áudio/TTS") {
            val ok = BridgeState.plugin?.testTtsFromPhone() == true
            if (!ok) {
                toast("Abra primeiro o plugin ChatGPT Bridge no Rokid Nexus/óculos.")
            }
        })

        promptText = EditText(this).apply {
            hint = "Frase para teste local no ChatGPT"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            setSingleLine(false)
            minLines = 2
            setPadding(10, 18, 10, 18)
        }
        content.addView(promptText)

        content.addView(button("7. Enviar teste ao ChatGPT") {
            val value = promptText.text?.toString()?.trim().orEmpty()
            if (value.isBlank()) {
                toast("Digite uma frase primeiro.")
            } else if (!isAccessibilityEnabled()) {
                toast("Ative primeiro a acessibilidade do Bridge.")
            } else {
                BridgeState.pendingPrompt = value
                DiagnosticStore.add(this, "Teste local preparado para o ChatGPT.")
                openPackage("com.openai.chatgpt", "ChatGPT oficial não encontrado.")
            }
        })

        content.addView(button("Atualizar status") {
            refreshStatus()
        })

        content.addView(button("Limpar log") {
            DiagnosticStore.clear(this)
            refreshStatus()
        })

        content.addView(TextView(this).apply {
            text = "LOG DE TESTE"
            textSize = 16f
            setTextColor(Color.WHITE)
            setPadding(0, 24, 0, 8)
        })

        logText = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.LTGRAY)
            setPadding(12, 12, 12, 12)
            setBackgroundColor(Color.rgb(18, 18, 18))
        }
        content.addView(logText)

        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.BLACK)
            addView(content)
        }
        setContentView(scroll)

        DiagnosticStore.add(this, "Painel v0.5 aberto.")
        refreshStatus()
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(refresher)
        handler.post(refresher)
    }

    override fun onPause() {
        handler.removeCallbacks(refresher)
        super.onPause()
    }

    private fun button(label: String, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            setOnClickListener { action() }
        }

    private fun refreshStatus() {
        val chatGpt = packageManager.getLaunchIntentForPackage("com.openai.chatgpt") != null
        val nexus = packageManager.getLaunchIntentForPackage("com.anezium.rokidbus.phone") != null
        val accessibility = isAccessibilityEnabled()
        val pluginOpen = BridgeState.plugin != null

        statusText.text = buildString {
            appendLine(if (chatGpt) "✅ ChatGPT instalado" else "❌ ChatGPT não encontrado")
            appendLine(if (nexus) "✅ Rokid Nexus instalado" else "❌ Rokid Nexus não encontrado")
            appendLine(if (accessibility) "✅ Acessibilidade ativa" else "❌ Acessibilidade desligada")
            append(if (pluginOpen) "✅ Plugin Nexus aberto/conectado" else "⚪ Plugin Nexus ainda não está aberto")
        }

        val log = DiagnosticStore.read(this)
        logText.text = if (log.isBlank()) "Nenhum evento registrado ainda." else log
    }

    private fun isAccessibilityEnabled(): Boolean {
        val expected = ComponentName(this, BridgeAccessibilityService::class.java)
            .flattenToString()
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty()

        return enabled.split(':').any {
            it.equals(expected, ignoreCase = true)
        }
    }

    private fun openPackage(packageName: String, error: String) {
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        if (launch == null) {
            toast(error)
            return
        }
        startActivity(launch)
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
