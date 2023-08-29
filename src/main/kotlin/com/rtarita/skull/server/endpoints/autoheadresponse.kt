package com.rtarita.skull.server.endpoints

import com.rtarita.skull.server.config.ConfigProvider
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.autohead.AutoHeadResponse

internal fun Application.initAutoHeadResponse() {
    if (ConfigProvider.featAutoHeadResponse) {
        install(AutoHeadResponse)
    }
}
