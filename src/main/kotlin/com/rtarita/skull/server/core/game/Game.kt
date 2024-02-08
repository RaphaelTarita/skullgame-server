package com.rtarita.skull.server.core.game

import com.rtarita.skull.common.state.PlayerInfo
import com.rtarita.skull.server.config.User
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.concurrent.ConcurrentHashMap

internal class Game(
    val gameid: String,
    initiator: User
) {
    private val players = ConcurrentHashMap<User, Int>()
    val mutex = Mutex()
    var lastInteraction: Instant = Clock.System.now()
    var isRunning = false
        private set
    private var playerCount = 0

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

    fun isPlaying(player: User) = players.containsKey(player)

    fun playerInfo() = players.map { (user, playerIndex) ->
        PlayerInfo(user.id, user.displayName, isInitiator(user), playerIndex)
    }

    fun isInitiator(player: User): Boolean = players[player] == 0

    suspend fun start(): GameController? {
        mutex.withLock {
            if (isRunning) return null
            isRunning = true
        }

        updateLastInteraction()
        return GameController(players.size)
    }
}
