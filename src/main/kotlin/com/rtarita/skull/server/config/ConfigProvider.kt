package com.rtarita.skull.server.config

import com.rtarita.skull.common.CommonConstants
import com.rtarita.skull.server.ServerConstants
import kotlin.io.path.bufferedReader

object ConfigProvider {
    private fun deserializeConfig(): Config = CommonConstants.json.decodeFromString(
        ServerConstants.configfilePath
            .bufferedReader()
            .readText()
    )

    private val deserializedConfig by lazy { deserializeConfig() }

    val authIssuer: String
        get() = deserializedConfig.auth.issuer

    val authAudience: String
        get() = deserializedConfig.auth.audience

    val authRealm: String
        get() = deserializedConfig.auth.realm

    val authKid: String
        get() = deserializedConfig.auth.kid

    val users: List<User>
        get() = deserializedConfig.users

    val featReverseProxy: Boolean
        get() = deserializedConfig.features.reverseProxy

    val featAutoHeadResponse: Boolean
        get() = deserializedConfig.features.autoHeadResponse
}