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

    const val CONFIG_DIR = "server-config"
    val configfilePath = Path("$CONFIG_DIR/config.json")
    val privatekeyPath = Path("$CONFIG_DIR/id_rsa")
    val certsPath = Path("$CONFIG_DIR/certs")
    val jwksPath = Path("$CONFIG_DIR/certs/jwks.json")

    val tokenExpiration: JDuration = JDuration.of(2, ChronoUnit.HOURS)
    val gameExpiration = with(KDuration) { 15.minutes }
    val gameExpirationUpdate = with(KDuration) { 5.minutes }
}