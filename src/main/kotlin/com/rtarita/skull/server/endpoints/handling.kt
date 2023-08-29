package com.rtarita.skull.server.endpoints

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.rtarita.skull.common.BadMove
import com.rtarita.skull.common.CommonConstants
import com.rtarita.skull.common.LoginCredentials
import com.rtarita.skull.common.Move
import com.rtarita.skull.common.TokenHolder
import com.rtarita.skull.server.ServerConstants
import com.rtarita.skull.server.auth.AuthStore
import com.rtarita.skull.server.auth.receiveUser
import com.rtarita.skull.server.core.game.GamesManager
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.util.pipeline.PipelineContext
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant

internal suspend fun PipelineContext<Unit, ApplicationCall>.handleLogin(authStore: AuthStore) {
    val cred = call.receive<LoginCredentials>()
    val user = authStore.authMap[cred.id to cred.passHash] ?: run {
        call.respond(HttpStatusCode.Unauthorized, "username or password wrong")
        return
    }

    val publicKey = authStore.jwkProvider.get(authStore.kid).publicKey
    val keySpecPKCS8 = PKCS8EncodedKeySpec(ServerConstants.b64decoder.decode(authStore.privateKeyString))
    val privateKey = ServerConstants.rsaKeyFactory.generatePrivate(keySpecPKCS8)
    val token = JWT.create()
        .withAudience(authStore.audience)
        .withIssuer(authStore.issuer)
        .withClaim(CommonConstants.USER_ID_CLAIM, user.id)
        .withExpiresAt(Instant.now() + ServerConstants.tokenExpiration)
        .sign(Algorithm.RSA256(publicKey as RSAPublicKey, privateKey as RSAPrivateKey))

    call.respond(TokenHolder(token))
}

internal suspend fun PipelineContext<Unit, ApplicationCall>.handleHello(authStore: AuthStore) {
    val user = receiveUser(authStore) ?: return
    call.respondText("Hello, ${user.displayName}!")
}

internal suspend fun PipelineContext<Unit, ApplicationCall>.handleNewgame(authStore: AuthStore) {
    val user = receiveUser(authStore) ?: return
    val gameid = GamesManager.newGame(user)
    call.respond(HttpStatusCode.Created, hashMapOf("gameid" to gameid))
}

internal suspend fun PipelineContext<Unit, ApplicationCall>.handleJoin(authStore: AuthStore) {
    val user = receiveUser(authStore) ?: return
    val gameid = call.parameters["gameid"]

    if (gameid == null || !GamesManager.joinGame(gameid, user)) {
        call.respond(
            HttpStatusCode.BadRequest,
            "game associated to '$gameid' does not exist, is not joinable or you already joined it"
        )
    } else {
        call.respond(HttpStatusCode.OK, "successfully joined the game")
    }
}

internal suspend fun PipelineContext<Unit, ApplicationCall>.handleStartgame(authStore: AuthStore) {
    val user = receiveUser(authStore) ?: return
    val gameid = call.parameters["gameid"]

    if (gameid == null || !GamesManager.startGame(gameid, user)) {
        call.respond(HttpStatusCode.NotModified, "game associated to '$gameid' could not be started by this user")
    } else {
        call.respond(HttpStatusCode.OK, "successfully started the game")
    }
}

internal suspend fun PipelineContext<Unit, ApplicationCall>.handleMove(authStore: AuthStore) {
    val user = receiveUser(authStore) ?: return
    val gameid = call.parameters["gameid"] ?: run {
        call.respond(HttpStatusCode.BadRequest, "no gameid given")
        return
    }
    val move = call.receive<Move>()

    when (val outcome = GamesManager.submitMove(gameid, user, move)) {
        is BadMove -> call.respond(HttpStatusCode.BadRequest, outcome)
        else -> call.respond(HttpStatusCode.OK, outcome)
    }
}

internal suspend fun PipelineContext<Unit, ApplicationCall>.handleMasterstate(authStore: AuthStore) {
    val user = receiveUser(authStore) ?: return
    if (!user.isAdmin) {
        call.respond(HttpStatusCode.Unauthorized, "cannot query master game state if not admin")
        return
    }

    val gameid = call.parameters["gameid"] ?: run {
        call.respond(HttpStatusCode.BadRequest, "no gameid given")
        return
    }

    val result = GamesManager.getMasterState(gameid)
    if (result != null) {
        call.respond(HttpStatusCode.OK, result)
    } else {
        call.respond(HttpStatusCode.BadRequest, "game associated to '$gameid' does not exist or was not yet started")
    }
}

internal suspend fun PipelineContext<Unit, ApplicationCall>.handleState(authStore: AuthStore) {
    val user = receiveUser(authStore) ?: return
    val gameid = call.parameters["gameid"] ?: run {
        call.respond(HttpStatusCode.BadRequest, "no gameid given")
        return
    }

    val result = GamesManager.getState(gameid, user)
    if (result != null) {
        call.respond(HttpStatusCode.OK, result)
    } else {
        call.respond(HttpStatusCode.BadRequest, "user is not a player in the game associated to '$gameid'")
    }
}
