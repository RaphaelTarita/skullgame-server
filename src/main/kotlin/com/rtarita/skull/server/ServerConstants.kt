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
    const val SITE_DIR = "site"
    val configfilePath = Path("$CONFIG_DIR/config.json")
    val privatekeyPath = Path("$CONFIG_DIR/id_rsa")
    val certsPath = Path("$CONFIG_DIR/certs")
    val jwksPath = Path("$CONFIG_DIR/certs/jwks.json")

    const val JWKP_CACHE_SIZE = 8L
    const val JWKP_CACHE_EXPIRATION_HOURS = 12L
    const val JWKP_RATELIMIT_BUCKET_SIZE = 10L
    const val JWKP_RATELIMIT_REFILL_RATE_MINUTES = 1L
    const val JWKP_VERIFY_LEEWAY_SECONDS = 3L

    val tokenExpiration: JDuration = JDuration.of(2, ChronoUnit.HOURS)
    val gameExpiration = with(KDuration) { 15.minutes }
    val gameExpirationUpdate = with(KDuration) { 5.minutes }
    const val GAME_ID_LENGTH = 8
}
