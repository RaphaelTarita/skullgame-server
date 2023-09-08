package com.rtarita.skull.server.core.game.handler

import com.rtarita.skull.common.moves.BadMove
import com.rtarita.skull.common.moves.Continue
import com.rtarita.skull.common.moves.Lay
import com.rtarita.skull.common.moves.MoveOutcome
import com.rtarita.skull.common.state.GameState
import com.rtarita.skull.common.state.TurnMode
import kotlin.random.Random

object LayHandler : MoveHandler<Lay> {
    override fun check(state: GameState, playerIndex: Int, move: Lay): BadMove? {
        if (state.currentTurnMode != TurnMode.LAY) {
            return BadMove("cannot lay card, game is in ${state.currentTurnMode} mode")
        }

        if (state.currentTurn != playerIndex) {
            return BadMove("cannot lay card, it is not player's turn")
        }

        if (move.card !in state.cardsInHand.getValue(playerIndex)) {
            return BadMove("cannot lay card, '${move.card}' is not in hand of player")
        }
        return null
    }

    override fun handle(state: GameState, playerIndex: Int, move: Lay, random: Random): Pair<GameState, MoveOutcome> {
        val (cardsInHand, cardsOnTable) = layCard(state.cardsInHand, state.cardsOnTable, playerIndex, move.card)
        val newState = state.copy(
            currentTurn = advanceTurn(state.currentTurn, state.numPlayers, state.cardsAvailable),
            cardsInHand = cardsInHand,
            cardsOnTable = cardsOnTable
        )

        return newState to Continue(newState.currentTurn)
    }
}
