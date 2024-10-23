package com.rtarita.skull.server.endpoints

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging
import org.slf4j.event.Level

internal fun Application.initCallLogging() {
    install(CallLogging) {
        level = Level.INFO
    }
}
