package com.rtarita.skull.server.endpoints.handling

import com.rtarita.skull.common.state.StateSignal
import com.rtarita.skull.server.auth.AuthStore
import com.rtarita.skull.server.auth.receiveUser
import com.rtarita.skull.server.core.state.StateSignalBroker
import io.ktor.server.application.log
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.Frame
import io.ktor.websocket.readBytes
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

private enum class WsLogType {
    CONN,
    SEND,
    RECEIVE
}

private fun DefaultWebSocketServerSession.wsInfo(type: WsLogType, msg: String) = call.application.log.info("WS $type - $msg")

internal suspend fun DefaultWebSocketServerSession.handleWebSocketSubscribe(authStore: AuthStore) {
    val user = call.receiveUser(authStore) ?: return
    wsInfo(WsLogType.CONN, "established connection with '${user.id}'")
    for (frame in incoming) {
        if (frame is Frame.Binary) {
            val signal = StateSignal.deserializeClient(frame.readBytes())
            wsInfo(WsLogType.RECEIVE, "$signal from '${user.id}'")
            when (signal) {
                is StateSignal.Client.RequestUpdates -> {
                    val flow = StateSignalBroker.register(signal.gameid, user) ?: continue
                    flow.onEach {
                        wsInfo(WsLogType.SEND, "$it to '${user.id}'")
                        send(Frame.Binary(true, it.serialize()))
                    }.launchIn(this)
                }

                is StateSignal.Client.StopUpdates -> StateSignalBroker.deregister(signal.gameid, user)
            }
            wsInfo(WsLogType.SEND, "${StateSignal.Server.Ack} to '${user.id}'")
            send(Frame.Binary(true, StateSignal.Server.Ack.serialize()))
        }
    }
    StateSignalBroker.deregisterAll(user)
    wsInfo(WsLogType.CONN, "lost connection with '${user.id}'")
}
