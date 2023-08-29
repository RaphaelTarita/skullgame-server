package com.rtarita.skull.server.endpoints

import com.rtarita.skull.server.config.ConfigProvider
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.forwardedheaders.ForwardedHeaders
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders

internal fun Application.initForwardedHeaders() {
    if (ConfigProvider.featReverseProxy) {
        install(ForwardedHeaders)
        install(XForwardedHeaders)
    }
}
