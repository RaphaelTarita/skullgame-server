package com.rtarita.skull.server.core.game

import com.rtarita.skull.server.config.User
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.concurrent.ConcurrentHashMap

internal class Game(
    val gameid: String,
    initiator: User
) {
    companion object {
        const val SIGNAL_REMOVE = "SIG#REM"
    }

    private val players = ConcurrentHashMap<User, Int>()
    val mutex = Mutex()
    var lastInteraction: Instant = Clock.System.now()
    private var isRunning = false
    private var playerCount = 0
    private val eventListeners = mutableListOf<suspend (String) -> Unit>()

    init {
        players[initiator] = playerCount++
    }

    suspend fun updateLastInteraction() = mutex.withLock {
        lastInteraction = Clock.System.now()
    }

    fun getPlayerIndex(user: User) = players[user]

    suspend fun joinGame(player: User): Boolean = mutex.withLock {
        if (isRunning) return false
        if (players.containsKey(player)) false else {
            players[player] = playerCount++
            true
        }
    }.also { updateLastInteraction() }

    fun isInitiator(player: User): Boolean = players[player] == 0

    suspend fun start(): GameController? {
        mutex.withLock {
            if (isRunning) return null
            isRunning = true
        }

        updateLastInteraction()
        return GameController(players.size)
    }

    suspend fun signalRemove() {
        for (listener in eventListeners) {
            coroutineScope {
                launch { listener(SIGNAL_REMOVE) }
            }
        }
    }
}
