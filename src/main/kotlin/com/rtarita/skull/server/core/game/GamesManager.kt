package com.rtarita.skull.server.core.game

import com.rtarita.skull.common.GameState
import com.rtarita.skull.common.Move
import com.rtarita.skull.common.MoveOutcome
import com.rtarita.skull.common.PlayerGameState
import com.rtarita.skull.server.config.ConfigProvider
import com.rtarita.skull.server.config.User
import com.rtarita.skull.server.core.state.GlobalState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
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

    suspend fun newGame(initiator: User): String = GlobalState.gameStore.newGame(initiator)

    suspend fun joinGame(gameid: String, user: User): Boolean = GlobalState.gameStore.joinGame(gameid, user)

    suspend fun startGame(gameid: String, user: User): Boolean = GlobalState.gameStore.startGame(gameid, user)

    suspend fun submitMove(gameid: String, user: User, move: Move): MoveOutcome = GlobalState.gameStore.submitMove(gameid, user, move)

    fun getMasterState(gameid: String): GameState? = GlobalState.gameStore.getMasterState(gameid)

    fun getState(gameid: String, user: User): PlayerGameState? = GlobalState.gameStore.getState(gameid, user)
}
