package ee.taltech.aireapplication.helpers

import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession

interface WebSocketListener {
    fun onConnected()
    fun onMessage(message: String)
    fun onDisconnected()
}