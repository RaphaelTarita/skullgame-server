package com.rtarita.skull.server.core.util

import kotlin.random.Random

private const val ALPHANUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

fun getRandomAlphanumString(random: Random, length: Int) = List(length) { ALPHANUM[random.nextInt(ALPHANUM.length)] }.joinToString("")