package com.rtarita.skull.server.core.game

import com.rtarita.skull.server.config.ConfigProvider
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
}
