package com.rtarita.skull.server.config

import kotlinx.serialization.Serializable

@Serializable
data class EnvironmentConfig(
    val host: String,
    val port: Int = 8443,
    val maxManagerThreads: Int = 4
)
