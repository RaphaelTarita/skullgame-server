package com.rtarita.skull.server.core.state

import com.rtarita.skull.common.condition.RendezvousSignalCondition
import com.rtarita.skull.common.condition.dsl.Wait
import com.rtarita.skull.common.condition.dsl.happens
import com.rtarita.skull.common.condition.dsl.until
import com.rtarita.skull.common.condition.rendezvousSignalCondition
import com.rtarita.skull.common.state.StateSignal
import com.rtarita.skull.server.config.User
import com.rtarita.skull.server.core.game.GamesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.transformWhile
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

internal object StateSignalBroker {
    private data class RegistryKey(val gameid: String, val user: User)

    private val logger = LoggerFactory.getLogger(this::class.java)
    private val registeredUsers = ConcurrentHashMap<RegistryKey, SharedFlow<StateSignal.Server>>()
    private val closeTokens = ConcurrentHashMap<RegistryKey, RendezvousSignalCondition>()

    private fun conditionFlow(key: RegistryKey): Flow<Nothing?>? {
        val closeSignal = closeTokens[key] ?: return null
        return flow {
            Wait until closeSignal.happens
            emit(null)
        }
    }

    private fun addRegistration(key: RegistryKey): Flow<StateSignal.Server>? {
        val signalFlow = registeredUsers.getOrPut(key) { GamesManager.getSignalFlow(key.gameid) ?: return null }
        closeTokens[key] = rendezvousSignalCondition()
        val closerFlow = conditionFlow(key) ?: return null
        return merge(signalFlow, closerFlow).transformWhile {
            if (it != null) emit(it)
            it != null && it !is StateSignal.Server.GameEnded
        }
    }

    private suspend fun removeRegistration(key: RegistryKey) {
        registeredUsers.remove(key)
        val token = closeTokens.remove(key)
        token?.signalAndClose()
    }

    fun register(gameid: String, user: User): Flow<StateSignal.Server>? {
        val result = addRegistration(RegistryKey(gameid, user))
        if (result != null) {
            logger.info("user '${user.id}' registered for updates on game with ID '$gameid'")
        }
        return result
    }

    suspend fun deregister(gameid: String, user: User) {
        removeRegistration(RegistryKey(gameid, user))
        logger.info("user '${user.id}' deregistered for updates on game with ID '$gameid'")
    }

    suspend fun deregisterAll(user: User) {
        registeredUsers.filterKeys { (_, registeredUser) -> registeredUser == user }
            .forEach { (key, _) -> removeRegistration(key) }
        logger.info("user '${user.id}' deregistered for all updates")
    }
}
