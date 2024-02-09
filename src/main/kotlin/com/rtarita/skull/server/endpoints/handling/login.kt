package com.rtarita.skull.server.endpoints.handling

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.rtarita.skull.common.CommonConstants
import com.rtarita.skull.common.auth.LoginCredentials
import com.rtarita.skull.common.auth.TokenHolder
import com.rtarita.skull.server.ServerConstants
import com.rtarita.skull.server.auth.AuthStore
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
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
