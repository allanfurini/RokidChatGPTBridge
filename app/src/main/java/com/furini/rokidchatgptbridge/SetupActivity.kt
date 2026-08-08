package com.furini.rokidchatgptbridge

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class SetupActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(28, 38, 28, 28)
            setBackgroundColor(Color.BLACK)
        }

        root.addView(TextView(this).apply {
            text = "Rokid ChatGPT Bridge"
            textSize = 24f
            setTextColor(Color.WHITE)
        })

        root.addView(TextView(this).apply {
            text = """
                v0.2 • sem OpenAI API

                Objetivo:
                Rokid → fala → celular → ChatGPT oficial → resposta → HUD Rokid

                1. Instale e configure o Rokid Nexus.
                2. Aprove este plugin e conceda Speech-to-text, Text-to-speech e Surfaces.
                3. Ative a acessibilidade deste app.
                4. Abra no celular a conversa do ChatGPT que você quer continuar.
                5. Abra "ChatGPT Bridge" pelo Nexus e fale.
            """.trimIndent()
            textSize = 16f
            setTextColor(Color.LTGRAY)
            setPadding(0, 28, 0, 28)
        })

        root.addView(Button(this).apply {
            text = "Ativar acessibilidade"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        })

        root.addView(Button(this).apply {
            text = "Abrir ChatGPT"
            setOnClickListener {
                val launch = packageManager.getLaunchIntentForPackage("com.openai.chatgpt")
                if (launch != null) startActivity(launch)
            }
        })

        setContentView(root)
    }
}
