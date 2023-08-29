package com.rtarita.skull.server.endpoints

import com.rtarita.skull.common.CommonConstants
import com.rtarita.skull.server.ServerConstants
import com.rtarita.skull.server.auth.AuthStore
import com.rtarita.skull.server.config.ConfigProvider
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.response.respond
import kotlin.io.path.reader

internal fun Application.initJwt(): AuthStore {
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
                acceptLeeway(ServerConstants.JWKP_VERIFY_LEEWAY_SECONDS)
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
