package com.rtarita.skull.server.core.game

import com.rtarita.skull.common.BadMove
import com.rtarita.skull.common.GameEnded
import com.rtarita.skull.common.GameState
import com.rtarita.skull.common.Move
import com.rtarita.skull.common.MoveOutcome
import com.rtarita.skull.common.PlayerGameState
import com.rtarita.skull.common.StateSignal
import com.rtarita.skull.server.ServerConstants
import com.rtarita.skull.server.config.User
import com.rtarita.skull.server.core.state.GlobalState
import com.rtarita.skull.server.util.getRandomAlphanumString
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import org.slf4j.LoggerFactory
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.asKotlinRandom

internal class GameStore {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val random = SecureRandom().asKotlinRandom()
    private val gamesMutex = Mutex()
    private val games = ConcurrentHashMap<String, Game>()
    private val signalFlows = ConcurrentHashMap<String, MutableSharedFlow<StateSignal.Server>>()
    private val controllers = ConcurrentHashMap<String, GameController>()

    private suspend fun signalStart(gameid: String) {
        signalFlows[gameid]?.emit(StateSignal.Server.GameStart(gameid))
    }

    private suspend fun signalUpdate(gameid: String) {
        signalFlows[gameid]?.emit(StateSignal.Server.GameUpdate(gameid))
    }

    private suspend fun signalEnded(gameid: String) {
        val flow = signalFlows.remove(gameid)
        flow?.emit(StateSignal.Server.GameEnded(gameid))
    }

    suspend fun scheduleCleanups() = coroutineScope {
        while (GlobalState.active) {
            cleanup()
            delay(ServerConstants.gameExpirationUpdate)
        }
    }

    suspend fun newGame(initiator: User): String = gamesMutex.withLock {
        var gameid: String
        do {
            gameid = getRandomAlphanumString(random, ServerConstants.GAME_ID_LENGTH)
        } while (games.containsKey(gameid))
        games[gameid] = Game(gameid, initiator)
        signalFlows[gameid] = MutableSharedFlow(
            replay = 0,
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        gameid
    }

    suspend fun joinGame(gameid: String, player: User): Boolean {
        val game = games[gameid] ?: return false
        return game.joinGame(player)
    }

    suspend fun startGame(gameid: String, player: User): Boolean {
        val game = games[gameid] ?: return false
        if (!game.isInitiator(player)) return false
        controllers[gameid] = game.start() ?: return false
        signalStart(gameid)
        return true
    }

    suspend fun submitMove(gameid: String, player: User, move: Move): MoveOutcome {
        val game = games[gameid] ?: return BadMove("game with ID '$gameid' does not exist")
        val controller = controllers[gameid] ?: return BadMove("game with ID '$gameid' has not started yet")
        val playerIndex = game.getPlayerIndex(player) ?: return BadMove("user with ID '${player.id}' is not part of this game")

        game.updateLastInteraction()
        val outcome = controller.tickGame(playerIndex, move)
        if (outcome is GameEnded) {
            game.mutex.withLock { removeGame(gameid) }
        } else {
            signalUpdate(gameid)
        }
        return outcome
    }

    fun getMasterState(gameid: String): GameState? {
        val controller = controllers[gameid] ?: return null

        return controller.getGameState()
    }

    fun getState(gameid: String, user: User): PlayerGameState? {
        val game = games[gameid] ?: return null
        val controller = controllers[gameid] ?: return null
        val playerIndex = game.getPlayerIndex(user) ?: return null

        return controller.getPlayerGameState(playerIndex)
    }

    fun getSignalFlow(gameid: String): SharedFlow<StateSignal.Server>? = signalFlows[gameid]?.asSharedFlow()

    private suspend fun cleanup() {
        for ((id, game) in games) {
            game.mutex.withLock {
                if (Clock.System.now() - game.lastInteraction > ServerConstants.gameExpiration) {
                    logger.info("removing game ${game.gameid} due to inactivity")
                    removeGame(id)
                }
            }
        }
    }

    private suspend fun removeGame(id: String) {
        games.remove(id)
        controllers.remove(id)
        signalEnded(id)
    }
}
