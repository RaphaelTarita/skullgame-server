package com.rtarita.skull.server.config

import kotlinx.serialization.Serializable

@Serializable
internal data class AuthConfig(
    val issuer: String,
    val audience: String,
    val realm: String,
    val kid: String,
)
