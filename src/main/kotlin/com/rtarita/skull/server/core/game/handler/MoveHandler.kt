package com.rtarita.skull.server.core.game.handler

import com.rtarita.skull.common.BadMove
import com.rtarita.skull.common.GameState
import com.rtarita.skull.common.Move
import com.rtarita.skull.common.MoveOutcome
import kotlin.random.Random

interface MoveHandler<T : Move> {
    fun check(state: GameState, playerIndex: Int, move: T): BadMove?

    fun handle(state: GameState, playerIndex: Int, move: T, random: Random): Pair<GameState, MoveOutcome>
}
