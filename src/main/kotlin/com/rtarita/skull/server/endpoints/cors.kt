package com.rtarita.skull.server.endpoints

import com.rtarita.skull.server.config.ConfigProvider
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS

internal fun Application.initCors() {
    val subdomains = ConfigProvider.envHost.split('.')

    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHost(subdomains.first(), listOf("https"), subdomains.drop(1))
    }
}
