package com.rtarita.skull.server.endpoints.handling

import com.rtarita.skull.common.moves.BadMove
import com.rtarita.skull.common.moves.Move
import com.rtarita.skull.server.auth.AuthStore
import com.rtarita.skull.server.auth.receiveUser
import com.rtarita.skull.server.core.state.GlobalState
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext

internal suspend fun RoutingContext.handlePlayerHello(authStore: AuthStore) {
    val user = call.receiveUser(authStore) ?: return
    call.respondText("Hello, ${user.displayName}!")
}

internal suspend fun RoutingContext.handlePlayerGames(authStore: AuthStore) {
    val user = call.receiveUser(authStore) ?: return
    val games = GlobalState.gameStore.gamesOf(user)
    call.respond(HttpStatusCode.OK, games)
}

internal suspend fun RoutingContext.handleNewgame(authStore: AuthStore) {
    val user = call.receiveUser(authStore) ?: return
    val gameid = GlobalState.gameStore.newGame(user)
    call.respond(HttpStatusCode.Created, hashMapOf("gameid" to gameid))
}

internal suspend fun RoutingContext.handleJoin(authStore: AuthStore) {
    val user = call.receiveUser(authStore) ?: return
    val gameid = call.parameters["gameid"]

    if (gameid == null || !GlobalState.gameStore.joinGame(gameid, user)) {
        call.respond(
            HttpStatusCode.BadRequest,
            "game associated to '$gameid' does not exist, is not joinable or you already joined it"
        )
    } else {
        call.respond(HttpStatusCode.OK, "successfully joined the game")
    }
}

internal suspend fun RoutingContext.handlePlayerinfo(authStore: AuthStore) {
    val user = call.receiveUser(authStore) ?: return
    val gameid = call.parameters["gameid"] ?: run {
        call.respond(HttpStatusCode.BadRequest, "no gameid given")
        return
    }

    val playerInfo = GlobalState.gameStore.playersOf(gameid, user)
    if (playerInfo != null) {
        call.respond(HttpStatusCode.OK, playerInfo)
    } else {
        call.respond(HttpStatusCode.BadRequest, "user is not a player in the game associated to '$gameid'")
    }
}

internal suspend fun RoutingContext.handleStartgame(authStore: AuthStore) {
    val user = call.receiveUser(authStore) ?: return
    val gameid = call.parameters["gameid"]

    if (gameid == null || !GlobalState.gameStore.startGame(gameid, user)) {
        call.respond(HttpStatusCode.NotModified, "game associated to '$gameid' could not be started by this user")
    } else {
        call.respond(HttpStatusCode.OK, "successfully started the game")
    }
}

internal suspend fun RoutingContext.handleMove(authStore: AuthStore) {
    val user = call.receiveUser(authStore) ?: return
    val gameid = call.parameters["gameid"] ?: run {
        call.respond(HttpStatusCode.BadRequest, "no gameid given")
        return
    }
    val move = call.receive<Move>()

    when (val outcome = GlobalState.gameStore.submitMove(gameid, user, move)) {
        is BadMove -> call.respond(HttpStatusCode.BadRequest, outcome)
        else -> call.respond(HttpStatusCode.OK, outcome)
    }
}

internal suspend fun RoutingContext.handleMasterstate(authStore: AuthStore) {
    val user = call.receiveUser(authStore) ?: return
    if (!user.isAdmin) {
        call.respond(HttpStatusCode.Unauthorized, "cannot query master game state if not admin")
        return
    }

    val gameid = call.parameters["gameid"] ?: run {
        call.respond(HttpStatusCode.BadRequest, "no gameid given")
        return
    }

    val result = GlobalState.gameStore.getMasterState(gameid)
    if (result != null) {
        call.respond(HttpStatusCode.OK, result)
    } else {
        call.respond(HttpStatusCode.BadRequest, "game associated to '$gameid' does not exist or was not yet started")
    }
}

internal suspend fun RoutingContext.handleState(authStore: AuthStore) {
    val user = call.receiveUser(authStore) ?: return
    val gameid = call.parameters["gameid"] ?: run {
        call.respond(HttpStatusCode.BadRequest, "no gameid given")
        return
    }

    val result = GlobalState.gameStore.getState(gameid, user)
    if (result != null) {
        call.respond(HttpStatusCode.OK, result)
    } else {
        call.respond(HttpStatusCode.BadRequest, "user is not a player in the game associated to '$gameid'")
    }
}
