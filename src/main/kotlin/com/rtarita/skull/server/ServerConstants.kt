package com.rtarita.skull.server

import java.security.KeyFactory
import java.time.temporal.ChronoUnit
import java.util.Base64
import kotlin.io.path.Path
import java.time.Duration as JDuration
import kotlin.time.Duration as KDuration

internal object ServerConstants {
    val b64decoder: Base64.Decoder = Base64.getDecoder()
    val rsaKeyFactory: KeyFactory = KeyFactory.getInstance("RSA")

    val configfilePath = Path("config/config.json")
    val privatekeyPath = Path("config/id_rsa")
    val certsPath = Path("config/certs")
    val jwksPath = Path("config/certs/jwks.json")

    val tokenExpiration: JDuration = JDuration.of(2, ChronoUnit.HOURS)
    val gameExpiration = with(KDuration) { 15.minutes }
    val gameExpirationUpdate = with(KDuration) { 5.minutes }
}