package com.furini.rokidchatgptbridge

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
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
import java.lang.reflect.Field
import java.lang.reflect.Method

class SetupActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var log: TextView
    private lateinit var prompt: EditText
    private val cxr by lazy { HiRokidCxrManager(this).also { BridgeState.cxrManager = it } }
    private var speech: CxrSpeechRecognizer? = null
    private val handler = Handler(Looper.getMainLooper())

    private val refresh = object : Runnable {
        override fun run() {
            updateStatus()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 30, 24, 40)
            setBackgroundColor(Color.BLACK)
        }

        root.addView(TextView(this).apply {
            text = "Rokid ChatGPT Bridge"
            textSize = 24f
            setTextColor(Color.WHITE)
        })
        root.addView(TextView(this).apply {
            text = "v0.6 • Hi Rokid CXR-L direto • sem API OpenAI"
            textSize = 14f
            setTextColor(Color.LTGRAY)
            setPadding(0, 4, 0, 16)
        })

        status = TextView(this).apply {
            textSize = 16f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 15)
        }
        root.addView(status)

        root.addView(btn("1. Ativar acessibilidade") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })

        root.addView(btn("2. Autorizar no Hi Rokid") {
            startHiRokidAuthorization()
        })

        root.addView(btn("3. Abrir Hi Rokid") {
            openPkg(HI_ROKID, "Hi Rokid não encontrado.")
        })

        root.addView(btn("4. Testar HUD no óculos") {
            cxr.showText(
                "TESTE HUD",
                "Se você lê isto no Rokid, o CXR-L direto está funcionando."
            )
        })

        root.addView(btn("5. Falar pelo microfone do Rokid") {
            requestPermissionsIfNeeded()
            speech?.stop()
            speech = CxrSpeechRecognizer(this, cxr) { text ->
                runOnUiThread {
                    cxr.showText("Você disse", text)
                    BridgeState.pendingPrompt = text
                    openPkg(CHATGPT, "ChatGPT não encontrado.")
                }
            }
            if (speech?.start() != true) {
                toast("Não consegui iniciar o teste de voz. Veja o LOG.")
            }
        })

        root.addView(btn("6. Parar microfone") {
            speech?.stop()
        })

        root.addView(btn("7. Abrir ChatGPT oficial") {
            openPkg(CHATGPT, "ChatGPT não encontrado.")
        })

        prompt = EditText(this).apply {
            hint = "Frase de teste para o ChatGPT"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
        }
        root.addView(prompt)

        root.addView(btn("8. Enviar texto de teste ao ChatGPT") {
            val s = prompt.text?.toString()?.trim().orEmpty()
            if (s.isBlank()) {
                toast("Digite uma frase.")
            } else {
                BridgeState.pendingPrompt = s
                openPkg(CHATGPT, "ChatGPT não encontrado.")
            }
        })

        root.addView(btn("Limpar log") {
            DiagnosticStore.clear(this)
            updateStatus()
        })

        root.addView(TextView(this).apply {
            text = "LOG"
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(0, 20, 0, 8)
        })

        log = TextView(this).apply {
            setTextColor(Color.LTGRAY)
            textSize = 12f
            setBackgroundColor(Color.rgb(18,18,18))
            setPadding(10,10,10,10)
        }
        root.addView(log)

        setContentView(ScrollView(this).apply {
            setBackgroundColor(Color.BLACK)
            addView(root)
        })

        DiagnosticStore.add(this, "v0.6 iniciada. Hi Rokid alvo: $HI_ROKID")
        requestPermissionsIfNeeded()
        updateStatus()
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(refresh)
        handler.post(refresh)
    }

    override fun onPause() {
        handler.removeCallbacks(refresh)
        super.onPause()
    }

    override fun onDestroy() {
        speech?.stop()
        super.onDestroy()
    }

    @Deprecated("Deprecated in Android SDK; retained for Hi Rokid auth result")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != AUTH_REQUEST_CODE) return

        val token = parseAuthToken(resultCode, data)
        if (token.isBlank()) {
            DiagnosticStore.add(this, "Autorização voltou sem token. Aprove a tela do Hi Rokid.")
            toast("Autorização não concluída.")
            return
        }

        getSharedPreferences("cxr", MODE_PRIVATE)
            .edit()
            .putString("token", token)
            .apply()

        DiagnosticStore.add(this, "Token Hi Rokid recebido (${token.length} chars).")
        cxr.connect(token)
    }

    private fun startHiRokidAuthorization() {
        try {
            val intent = Intent().apply {
                component = ComponentName(
                    HI_ROKID,
                    "com.rokid.sprite.aiapp.externalapp.auth.AuthorizationActivity"
                )
            }
            DiagnosticStore.add(this, "Abrindo autorização do Hi Rokid...")
            startActivityForResult(intent, AUTH_REQUEST_CODE)
        } catch (t: Throwable) {
            DiagnosticStore.add(this, "Falha ao abrir autorização: ${t.message}")
            toast("Não consegui abrir a autorização do Hi Rokid.")
        }
    }

    private fun parseAuthToken(resultCode: Int, data: Intent?): String {
        try {
            val helperClass = Class.forName(
                "com.rokid.sprite.aiapp.externalapp.auth.AuthorizationHelper"
            )
            val instanceField: Field = helperClass.getDeclaredField("INSTANCE")
            val helper = instanceField.get(null)
            val parse: Method = helperClass.getMethod(
                "parseAuthorizationResult",
                Int::class.javaPrimitiveType,
                Intent::class.java
            )
            val result = parse.invoke(helper, resultCode, data)
            if (result != null && result.javaClass.simpleName == "AuthSuccess") {
                for (name in listOf("token", "authToken")) {
                    try {
                        val f = result.javaClass.getDeclaredField(name)
                        f.isAccessible = true
                        val v = f.get(result)
                        if (v is String && v.isNotBlank()) return v
                    } catch (_: Throwable) {}
                }
                for (name in listOf("getToken", "getAuthToken", "getAccessToken")) {
                    try {
                        val m = result.javaClass.getMethod(name)
                        val v = m.invoke(result)
                        if (v is String && v.isNotBlank()) return v
                    } catch (_: Throwable) {}
                }
            }
        } catch (t: Throwable) {
            DiagnosticStore.add(this, "Parser oficial indisponível: ${t.javaClass.simpleName}")
        }
        return data?.getStringExtra("auth_token").orEmpty()
    }

    private fun updateStatus() {
        val hi = packageManager.getLaunchIntentForPackage(HI_ROKID) != null
        val chat = packageManager.getLaunchIntentForPackage(CHATGPT) != null
        val access = isAccessibilityEnabled()
        val savedToken = getSharedPreferences("cxr", MODE_PRIVATE)
            .getString("token", null) != null

        status.text = buildString {
            appendLine(if (hi) "✅ Hi Rokid encontrado" else "❌ Hi Rokid não encontrado")
            appendLine(if (chat) "✅ ChatGPT instalado" else "❌ ChatGPT não encontrado")
            appendLine(if (access) "✅ Acessibilidade ativa" else "❌ Acessibilidade desligada")
            appendLine(if (savedToken) "✅ Hi Rokid já autorizou o Bridge" else "⚪ Falta autorizar no Hi Rokid")
            appendLine(if (BridgeState.glassBtConnected) "✅ Óculos Bluetooth detectados" else "⚪ Aguardando conexão dos óculos")
            appendLine(if (BridgeState.cxrConnected) "✅ CXR-L conectado" else "⚪ CXR-L ainda não conectado")
            append(if (BridgeState.sceneReady) "✅ HUD pronto" else "⚪ HUD ainda não abriu")
        }

        log.text = DiagnosticStore.read(this).ifBlank { "Nenhum evento." }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val expected = ComponentName(this, BridgeAccessibilityService::class.java)
            .flattenToString()
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty()
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    private fun requestPermissionsIfNeeded() {
        val wanted = mutableListOf<String>()
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            wanted += Manifest.permission.RECORD_AUDIO
        }
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            wanted += Manifest.permission.BLUETOOTH_CONNECT
        }
        if (wanted.isNotEmpty()) requestPermissions(wanted.toTypedArray(), 77)
    }

    private fun openPkg(pkg: String, error: String) {
        val launch = packageManager.getLaunchIntentForPackage(pkg)
        if (launch == null) {
            toast(error)
            return
        }
        startActivity(launch)
    }

    private fun btn(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        gravity = Gravity.CENTER
        setOnClickListener { action() }
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()

    companion object {
        private const val HI_ROKID = "com.rokid.sprite.global.aiapp"
        private const val CHATGPT = "com.openai.chatgpt"
        private const val AUTH_REQUEST_CODE = 4027
    }
}
