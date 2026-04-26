package com.bbsrevival.data.api

import android.util.Log
import com.bbsrevival.BuildConfig
import com.bbsrevival.data.models.ChatMessage
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

data class PresenceEvent(val userId: String, val handle: String, val status: String)
data class DoorOutput(val gameSlug: String, val output: String)
data class DoorEnd(val gameSlug: String, val score: Int?)

@Singleton
class SocketManager @Inject constructor(private val tokenStore: TokenStore) {

    private var socket: Socket? = null

    // Exposed flows
    private val _connected        = MutableStateFlow(false)
    private val _messages         = MutableSharedFlow<ChatMessage>(extraBufferCapacity = 64)
    private val _presence         = MutableSharedFlow<PresenceEvent>(extraBufferCapacity = 32)
    private val _doorOutput       = MutableSharedFlow<DoorOutput>(extraBufferCapacity = 128)
    private val _doorEnd          = MutableSharedFlow<DoorEnd>(extraBufferCapacity = 8)
    private val _userJoined       = MutableSharedFlow<Pair<String,String>>(extraBufferCapacity = 16) // roomId, handle
    private val _userLeft         = MutableSharedFlow<Pair<String,String>>(extraBufferCapacity = 16)
    private val _typingUsers      = MutableSharedFlow<Pair<String,String>>(extraBufferCapacity = 32) // roomId, handle

    val connected:   StateFlow<Boolean>         = _connected
    val messages:    SharedFlow<ChatMessage>    = _messages
    val presence:    SharedFlow<PresenceEvent>  = _presence
    val doorOutput:  SharedFlow<DoorOutput>     = _doorOutput
    val doorEnd:     SharedFlow<DoorEnd>        = _doorEnd
    val userJoined:  SharedFlow<Pair<String,String>> = _userJoined
    val userLeft:    SharedFlow<Pair<String,String>> = _userLeft
    val typingUsers: SharedFlow<Pair<String,String>> = _typingUsers

    fun connect() {
        val token = tokenStore.accessToken ?: return
        if (socket?.connected() == true) return

        val opts = IO.Options.builder()
            .setAuth(mapOf("token" to token))
            .setTransports(arrayOf("websocket"))
            .setReconnection(true)
            .setReconnectionAttempts(10)
            .setReconnectionDelay(2000)
            .build()

        socket = IO.socket(URI.create(BuildConfig.API_BASE_URL), opts).apply {
            on(Socket.EVENT_CONNECT)    { _connected.value = true }
            on(Socket.EVENT_DISCONNECT) { _connected.value = false }

            on("chat:message") { args ->
                val obj = args[0] as? JSONObject ?: return@on
                _messages.tryEmit(obj.toChatMessage())
            }

            on("chat:user_joined") { args ->
                val obj = args[0] as? JSONObject ?: return@on
                _userJoined.tryEmit(obj.getString("roomId") to obj.getString("handle"))
            }

            on("chat:user_left") { args ->
                val obj = args[0] as? JSONObject ?: return@on
                _userLeft.tryEmit(obj.getString("roomId") to obj.getString("handle"))
            }

            on("chat:typing") { args ->
                val obj = args[0] as? JSONObject ?: return@on
                _typingUsers.tryEmit(obj.getString("roomId") to obj.getString("handle"))
            }

            on("presence:update") { args ->
                val obj = args[0] as? JSONObject ?: return@on
                _presence.tryEmit(PresenceEvent(
                    userId = obj.getString("userId"),
                    handle = obj.getString("handle"),
                    status = obj.getString("status"),
                ))
            }

            on("door:output") { args ->
                val obj = args[0] as? JSONObject ?: return@on
                _doorOutput.tryEmit(DoorOutput(
                    gameSlug = obj.getString("gameSlug"),
                    output   = obj.getString("output"),
                ))
            }

            on("door:end") { args ->
                val obj = args[0] as? JSONObject ?: return@on
                _doorEnd.tryEmit(DoorEnd(
                    gameSlug = obj.getString("gameSlug"),
                    score    = if (obj.has("score") && !obj.isNull("score")) obj.getInt("score") else null,
                ))
            }

            on("error") { args ->
                Log.w("BbsSocket", "Server error: ${args.firstOrNull()}")
            }

            connect()
        }
    }

    fun disconnect() {
        socket?.disconnect()
        socket = null
        _connected.value = false
    }

    fun joinRoom(roomId: String) {
        socket?.emit("chat:join", roomId)
    }

    fun leaveRoom(roomId: String) {
        socket?.emit("chat:leave", roomId)
    }

    fun sendChatMessage(roomId: String, body: String) {
        val payload = JSONObject().apply { put("roomId", roomId); put("body", body) }
        socket?.emit("chat:message", payload)
    }

    fun sendTyping(roomId: String) {
        socket?.emit("chat:typing", roomId)
    }

    fun sendPresencePing() {
        socket?.emit("presence:ping")
    }

    fun startDoorGame(gameSlug: String) {
        val payload = JSONObject().apply {
            put("gameSlug", gameSlug)
            put("input", "__START__")
        }
        socket?.emit("door:input", payload)
    }

    fun sendDoorInput(gameSlug: String, input: String) {
        val payload = JSONObject().apply {
            put("gameSlug", gameSlug)
            put("input", input)
        }
        socket?.emit("door:input", payload)
    }

    private fun JSONObject.toChatMessage() = ChatMessage(
        id         = getString("id"),
        roomId     = getString("roomId"),
        userId     = getString("userId"),
        userHandle = getString("userHandle"),
        userRole   = getString("userRole"),
        body       = getString("body"),
        createdAt  = getString("createdAt"),
    )
}
