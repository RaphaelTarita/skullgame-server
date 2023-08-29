package com.rtarita.skull.server.config

import kotlinx.serialization.Serializable

@Serializable
data class EnvironmentConfig(
    val maxManagerThreads: Int = 4
)
