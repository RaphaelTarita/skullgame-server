package com.rtarita.skull.server.endpoints

import com.rtarita.skull.common.CommonConstants
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation

internal fun Application.initContentNegotiation() {
    install(ContentNegotiation) {
        json(CommonConstants.json)
    }
}
