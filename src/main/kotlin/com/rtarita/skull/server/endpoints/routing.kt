package com.rtarita.skull.server.endpoints

import com.rtarita.skull.server.ServerConstants
import com.rtarita.skull.server.auth.AuthStore
import com.rtarita.skull.server.config.ConfigProvider
import com.rtarita.skull.server.endpoints.handling.handleJoin
import com.rtarita.skull.server.endpoints.handling.handleLogin
import com.rtarita.skull.server.endpoints.handling.handleMasterstate
import com.rtarita.skull.server.endpoints.handling.handleMove
import com.rtarita.skull.server.endpoints.handling.handleNewgame
import com.rtarita.skull.server.endpoints.handling.handlePlayerGames
import com.rtarita.skull.server.endpoints.handling.handlePlayerHello
import com.rtarita.skull.server.endpoints.handling.handlePlayerinfo
import com.rtarita.skull.server.endpoints.handling.handleStartgame
import com.rtarita.skull.server.endpoints.handling.handleState
import com.rtarita.skull.server.endpoints.handling.handleWebSocketSubscribe
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.http.content.staticFiles
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.websocket.webSocket
import java.io.File
import kotlin.io.path.Path

internal fun Application.initRouting(authStore: AuthStore) = routing {
    publicRoutes(authStore)
    authenticatedRoutes(authStore)
    staticRoutes()
}

private fun Routing.publicRoutes(authStore: AuthStore) {
    post("/login") {
        handleLogin(authStore)
    }
}

private fun Routing.authenticatedRoutes(authStore: AuthStore) = authenticate("auth-jwt") {
    webSocket("/subscribe") {
        handleWebSocketSubscribe(authStore)
    }

    route("/player") {
        get("/hello") {
            handlePlayerHello(authStore)
        }
        get("/games") {
            handlePlayerGames(authStore)
        }
    }

    post("/newgame") {
        handleNewgame(authStore)
    }

    post("/join/{gameid}") {
        handleJoin(authStore)
    }

    get("/playerinfo/{gameid}") {
        handlePlayerinfo(authStore)
    }

    post("/startgame/{gameid}") {
        handleStartgame(authStore)
    }

    post("/move/{gameid}") {
        handleMove(authStore)
    }

    get("/masterstate/{gameid}") {
        handleMasterstate(authStore)
    }

    get("/state/{gameid}") {
        handleState(authStore)
    }
}

private fun Routing.staticRoutes() {
    staticFiles("/.well-known", ServerConstants.certsPath.toFile()) {
        exclude { Path(it.path) != ServerConstants.jwksPath }
    }

    if (ConfigProvider.featSiteServing) {
        staticFiles("/", File(ServerConstants.SITE_DIR))
    }
}
