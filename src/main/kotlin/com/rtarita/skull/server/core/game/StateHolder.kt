package com.rtarita.skull.server.core.game

import com.rtarita.skull.common.GameState
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class StateHolder(@PublishedApi internal var state: GameState) {
    @PublishedApi
    internal val mutex = Mutex()

    fun get() = state

    suspend inline fun <T> modifyState(modify: (GameState) -> Pair<GameState, T>): T = mutex.withLock {
        val (newState, result) = modify(state)
        state = newState
        result
    }
}
