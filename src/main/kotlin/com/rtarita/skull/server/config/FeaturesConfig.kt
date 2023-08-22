package com.rtarita.skull.server.config

import kotlinx.serialization.Serializable

@Serializable
internal data class FeaturesConfig(
    val reverseProxy: Boolean,
    val autoHeadResponse: Boolean
)
