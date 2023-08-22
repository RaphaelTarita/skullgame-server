package com.rtarita.skull.server.config

import kotlinx.serialization.Serializable

@Serializable
internal data class Config(
    val auth: AuthConfig,
    val users: List<User>,
    val features: FeaturesConfig
)