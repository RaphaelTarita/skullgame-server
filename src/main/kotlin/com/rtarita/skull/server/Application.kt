package com.rtarita.skull.server

import com.rtarita.skull.server.auth.AuthStore
import com.rtarita.skull.server.core.game.GamesManager
import com.rtarita.skull.server.endpoints.initAutoHeadResponse
import com.rtarita.skull.server.endpoints.initCallLogging
import com.rtarita.skull.server.endpoints.initContentNegotiation
import com.rtarita.skull.server.endpoints.initCors
import com.rtarita.skull.server.endpoints.initDefaultHeaders
import com.rtarita.skull.server.endpoints.initForwardedHeaders
import com.rtarita.skull.server.endpoints.initJwt
import com.rtarita.skull.server.endpoints.initRouting
import com.rtarita.skull.server.endpoints.initStatusPages
import io.ktor.server.application.Application
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer


fun main() {
    embeddedServer(CIO, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
    GamesManager.deactivate()
}

fun Application.module() {
    val authStore = initPlugins()

    initRouting(authStore)

    GamesManager.activate()
}

private fun Application.initPlugins(): AuthStore {
    val authStore = initJwt()
    initCors()
    initDefaultHeaders()
    initForwardedHeaders()
    initCallLogging()
    initAutoHeadResponse()
    initStatusPages()
    initContentNegotiation()

    return authStore
}
