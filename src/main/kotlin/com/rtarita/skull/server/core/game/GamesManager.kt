package com.rtarita.skull.server.core.game

import com.rtarita.skull.common.moves.Move
import com.rtarita.skull.common.moves.MoveOutcome
import com.rtarita.skull.common.state.GameState
import com.rtarita.skull.common.state.PlayerGame
import com.rtarita.skull.common.state.PlayerGameState
import com.rtarita.skull.common.state.PlayerInfo
import com.rtarita.skull.common.state.StateSignal
import com.rtarita.skull.server.config.ConfigProvider
import com.rtarita.skull.server.config.User
import com.rtarita.skull.server.core.state.GlobalState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import kotlin.math.min

internal object GamesManager {
    private val threadpool by lazy {
        Executors.newFixedThreadPool(min(ConfigProvider.envMaxManagerThreads, Runtime.getRuntime().availableProcessors()))
    }
    private val coroutineScope by lazy { CoroutineScope(threadpool.asCoroutineDispatcher()) }

    fun activate() {
        GlobalState.active = true
        coroutineScope.launch {
            GlobalState.gameStore.scheduleCleanups()
        }
    }

    fun deactivate() {
        GlobalState.active = false
        coroutineScope.cancel()
        threadpool.shutdown()
    }

    fun gamesOf(user: User): List<PlayerGame> = GlobalState.gameStore.gamesOf(user)

    fun playersOf(gameid: String, user: User): List<PlayerInfo>? = GlobalState.gameStore.playersOf(gameid, user)

    suspend fun newGame(initiator: User): String = GlobalState.gameStore.newGame(initiator)

    suspend fun joinGame(gameid: String, user: User): Boolean = GlobalState.gameStore.joinGame(gameid, user)

    suspend fun startGame(gameid: String, user: User): Boolean = GlobalState.gameStore.startGame(gameid, user)

    suspend fun submitMove(gameid: String, user: User, move: Move): MoveOutcome = GlobalState.gameStore.submitMove(gameid, user, move)

    fun getMasterState(gameid: String): GameState? = GlobalState.gameStore.getMasterState(gameid)

    fun getState(gameid: String, user: User): PlayerGameState? = GlobalState.gameStore.getState(gameid, user)

    fun getSignalFlow(gameid: String): SharedFlow<StateSignal.Server>? = GlobalState.gameStore.getSignalFlow(gameid)
}
