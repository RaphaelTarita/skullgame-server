package com.rtarita.skull.server.config

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val passHash: String,
    val displayName: String,
    val isAdmin: Boolean = false
)