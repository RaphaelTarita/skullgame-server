package com.rtarita.skull.server.util

inline fun <K, V> Map<K, V>.mutate(key: K, mutation: (Pair<K, V>) -> V): Map<K, V> {
    val value = getValue(key)
    return toMutableMap().also {
        it[key] = mutation(key to value)
    }
}