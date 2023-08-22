package com.rtarita.skull.server

import com.rtarita.skull.common.BadMove
import com.rtarita.skull.common.CommonConstants
import com.rtarita.skull.common.Move
import com.rtarita.skull.server.auth.AuthStore
import com.rtarita.skull.server.auth.login
import com.rtarita.skull.server.auth.receiveUser
import com.rtarita.skull.server.config.ConfigProvider
import com.rtarita.skull.server.core.game.GamesManager
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticFiles
import io.ktor.server.plugins.autohead.AutoHeadResponse
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.forwardedheaders.ForwardedHeaders
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import org.slf4j.event.Level
import kotlin.io.path.Path
import kotlin.io.path.reader


fun main() {
    embeddedServer(CIO, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
    GamesManager.deactivate()
}

fun Application.module() {
    val authStore = initPlugins()

    routing {
        post("/login") {
            login(authStore)
        }

        authenticate("auth-jwt") {
            get("/hello") {
                val user = receiveUser(authStore) ?: return@get
                call.respondText("Hello, ${user.displayName}!")
            }

            post("/newgame") {
                val user = receiveUser(authStore) ?: return@post
                val gameid = GamesManager.newGame(user)
                call.respond(HttpStatusCode.Created, hashMapOf("gameid" to gameid))
            }

            post("/join/{gameid}") {
                val user = receiveUser(authStore) ?: return@post
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

            post("/startgame/{gameid}") {
                val user = receiveUser(authStore) ?: return@post
                val gameid = call.parameters["gameid"]

                if (gameid == null || !GamesManager.startGame(gameid, user)) {
                    call.respond(HttpStatusCode.NotModified, "game associated to '$gameid' could not be started by this user")
                } else {
                    call.respond(HttpStatusCode.OK, "successfully started the game")
                }
            }

            post("/move/{gameid}") {
                val user = receiveUser(authStore) ?: return@post
                val gameid = call.parameters["gameid"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, "no gameid given")
                    return@post
                }
                val move = call.receive<Move>()

                when (val outcome = GamesManager.submitMove(gameid, user, move)) {
                    is BadMove -> call.respond(HttpStatusCode.BadRequest, outcome)
                    else -> call.respond(HttpStatusCode.OK, outcome)
                }
            }

            get("/masterstate/{gameid}") {
                val user = receiveUser(authStore) ?: return@get
                if (!user.isAdmin) {
                    call.respond(HttpStatusCode.Unauthorized, "cannot query master game state if not admin")
                    return@get
                }

                val gameid = call.parameters["gameid"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, "no gameid given")
                    return@get
                }

                val result = GamesManager.getMasterState(gameid)
                if (result != null) {
                    call.respond(HttpStatusCode.OK, result)
                } else {
                    call.respond(HttpStatusCode.BadRequest, "game associated to '$gameid' does not exist or was not yet started")
                }
            }

            get("/state/{gameid}") {
                val user = receiveUser(authStore) ?: return@get
                val gameid = call.parameters["gameid"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, "no gameid given")
                    return@get
                }

                val result = GamesManager.getState(gameid, user)
                if (result != null) {
                    call.respond(HttpStatusCode.OK, result)
                } else {
                    call.respond(HttpStatusCode.BadRequest, "user is not a player in the game associated to '$gameid'")
                }
            }
        }

        staticFiles("/.well-known", ServerConstants.certsPath.toFile()) {
            exclude { Path(it.path) != ServerConstants.jwksPath }
        }
    }

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

private fun Application.initJwt(): AuthStore {
    val authStore = AuthStore(
        ServerConstants.privatekeyPath.reader().readText().replace("\n", ""),
        ConfigProvider.authIssuer,
        ConfigProvider.authAudience,
        ConfigProvider.authRealm,
        ConfigProvider.authKid
    )

    install(Authentication) {
        jwt("auth-jwt") {
            realm = authStore.realm
            verifier(authStore.jwkProvider, authStore.issuer) {
                acceptLeeway(3)
            }
            validate { cred ->
                val userid = cred.payload.getClaim(CommonConstants.USER_ID_CLAIM).asString()
                if (authStore.userMap.containsKey(userid)) JWTPrincipal(cred.payload) else null
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, "invalid token")
            }
        }
    }

    return authStore
}

private fun Application.initCors() {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        allowHost("rtarita.com", listOf("https"), listOf("skull"))
    }
}

private fun Application.initDefaultHeaders() {
    install(DefaultHeaders) {
        header("X-Engine", "Ktor") // will send this header with each response
    }
}

private fun Application.initForwardedHeaders() {
    if (ConfigProvider.featReverseProxy) {
        install(ForwardedHeaders)
        install(XForwardedHeaders)
    }
}

private fun Application.initCallLogging() {
    install(CallLogging) {
        level = Level.INFO
    }
}

private fun Application.initAutoHeadResponse() {
    if (ConfigProvider.featAutoHeadResponse) {
        install(AutoHeadResponse)
    }
}

private fun Application.initStatusPages() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
    }
}

private fun Application.initContentNegotiation() {
    install(ContentNegotiation) {
        json(CommonConstants.json)
    }
}
