package com.rtarita.skull.server.auth

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.rtarita.skull.server.ServerConstants
import com.rtarita.skull.server.config.ConfigProvider
import com.rtarita.skull.server.config.User
import java.util.concurrent.TimeUnit

internal class AuthStore(
    val privateKeyString: String,
    val issuer: String,
    val audience: String,
    val realm: String,
    val kid: String
) {
    val jwkProvider: JwkProvider by lazy {
        JwkProviderBuilder(issuer)
            .cached(ServerConstants.JWKP_CACHE_SIZE, ServerConstants.JWKP_CACHE_EXPIRATION_HOURS, TimeUnit.HOURS)
            .rateLimited(ServerConstants.JWKP_RATELIMIT_BUCKET_SIZE, ServerConstants.JWKP_RATELIMIT_REFILL_RATE_MINUTES, TimeUnit.MINUTES)
            .build()
    }

    val authMap: Map<Pair<String, String>, User> by lazy {
        ConfigProvider.users.associateBy { it.id to it.passHash }
    }

    val userMap: Map<String, User> by lazy {
        ConfigProvider.users.associateBy { it.id }
    }
}
