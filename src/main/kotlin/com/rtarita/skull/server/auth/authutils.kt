package com.rtarita.skull.server.auth

import com.rtarita.skull.common.CommonConstants
import com.rtarita.skull.server.config.User
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond

internal suspend fun ApplicationCall.receiveUser(authStore: AuthStore): User? {
    val userid = principal<JWTPrincipal>()?.payload
        ?.getClaim(CommonConstants.USER_ID_CLAIM)
        ?.asString()
        ?: run {
            respond(HttpStatusCode.Unauthorized, "unknown user ID")
            return null
        }

    return authStore.userMap[userid]
}
