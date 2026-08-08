package com.furini.rokidchatgptbridge

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DiagnosticStore {
    private const val PREFS = "bridge_diagnostics"
    private const val KEY_LOG = "log"
    private const val MAX_LINES = 40

    fun add(context: Context, message: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val old = prefs.getString(KEY_LOG, "").orEmpty()
        val lines = (old.lines().filter { it.isNotBlank() } + "[$now] $message")
            .takeLast(MAX_LINES)
        prefs.edit().putString(KEY_LOG, lines.joinToString("\n")).apply()
    }

    fun read(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LOG, "")
            .orEmpty()

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_LOG)
            .apply()
    }
}
