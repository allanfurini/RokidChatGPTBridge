package com.furini.rokidchatgptbridge

object BridgeState {
    @Volatile var latestVisibleText: String = ""
    @Volatile var pendingPrompt: String? = null
    @Volatile var cxrConnected: Boolean = false
    @Volatile var glassBtConnected: Boolean = false
    @Volatile var sceneReady: Boolean = false
    @Volatile var lastSpeechText: String = ""
    @Volatile var cxrManager: HiRokidCxrManager? = null
}
