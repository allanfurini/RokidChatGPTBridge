package com.furini.rokidchatgptbridge

object BridgeState {
    @Volatile var plugin: RokidBridgePluginService? = null
    @Volatile var latestVisibleText: String = ""
    @Volatile var pendingPrompt: String? = null
}
