package com.rtarita.skull.server.config

import kotlinx.serialization.Serializable

@Serializable
internal data class Config(
    val auth: AuthConfig,
    val users: List<User>,
    val environment: EnvironmentConfig,
    val features: FeaturesConfig = FeaturesConfig(),
)
