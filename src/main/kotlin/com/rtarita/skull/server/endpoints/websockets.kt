package com.rtarita.skull.server.endpoints

import com.rtarita.skull.common.CommonConstants
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout

internal fun Application.initWebSockets() {
    install(WebSockets) {
        pingPeriod = CommonConstants.WEBSOCKET_PING_PERIOD
        timeout = CommonConstants.WEBSOCKET_TIMEOUT
        maxFrameSize = CommonConstants.WEBSOCKET_MAX_FRAME_SIZE
        masking = true
    }
}
