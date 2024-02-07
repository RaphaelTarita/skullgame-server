package com.rtarita.skull.server.config

import kotlinx.serialization.Serializable

@Serializable
internal data class FeaturesConfig(
    val reverseProxy: Boolean = false,
    val autoHeadResponse: Boolean = true,
    val siteServing: Boolean = false
)
