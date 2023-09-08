package com.rtarita.skull.server.endpoints

import com.rtarita.skull.common.CommonConstants
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import java.time.Duration

internal fun Application.initWebSockets() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(CommonConstants.WEBSOCKET_PING_PERIOD_SECONDS)
        timeout = Duration.ofSeconds(CommonConstants.WEBSOCKET_TIMEOUT_SECONDS)
        maxFrameSize = CommonConstants.WEBSOCKET_MAX_FRAME_SIZE
        masking = true
    }
}
