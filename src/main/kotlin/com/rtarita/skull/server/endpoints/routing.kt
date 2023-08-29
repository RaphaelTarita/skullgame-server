package com.rtarita.skull.server.endpoints

import com.rtarita.skull.server.ServerConstants
import com.rtarita.skull.server.auth.AuthStore
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.http.content.staticFiles
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
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
    get("/hello") {
        handleHello(authStore)
    }

    post("/newgame") {
        handleNewgame(authStore)
    }

    post("/join/{gameid}") {
        handleJoin(authStore)
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
}
