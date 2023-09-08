package com.rtarita.skull.server.core.game.handler

import com.rtarita.skull.common.moves.BadMove
import com.rtarita.skull.common.moves.Move
import com.rtarita.skull.common.moves.MoveOutcome
import com.rtarita.skull.common.state.GameState
import kotlin.random.Random

interface MoveHandler<T : Move> {
    fun check(state: GameState, playerIndex: Int, move: T): BadMove?

    fun handle(state: GameState, playerIndex: Int, move: T, random: Random): Pair<GameState, MoveOutcome>
}
