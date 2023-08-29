package com.rtarita.skull.server.core.game.handler

import com.rtarita.skull.common.BadMove
import com.rtarita.skull.common.Continue
import com.rtarita.skull.common.FirstCard
import com.rtarita.skull.common.GameState
import com.rtarita.skull.common.MoveOutcome
import com.rtarita.skull.common.TurnMode
import kotlin.random.Random

object FirstCardHandler : MoveHandler<FirstCard> {
    override fun check(state: GameState, playerIndex: Int, move: FirstCard): BadMove? {
        if (state.currentTurnMode != TurnMode.FIRST_CARD) {
            return BadMove("cannot lay first card, game is in ${state.currentTurnMode} mode")
        }

        if (state.cardsOnTable.getValue(playerIndex).isNotEmpty()) {
            return BadMove("cannot lay first card, was already laid by player")
        }

        if (move.card !in state.cardsInHand.getValue(playerIndex)) {
            return BadMove("cannot lay first card, '${move.card}' is not in hand of player")
        }
        return null
    }

    override fun handle(state: GameState, playerIndex: Int, move: FirstCard, random: Random): Pair<GameState, MoveOutcome> {
        val (cardsInHand, cardsOnTable) = layCard(state.cardsInHand, state.cardsOnTable, playerIndex, move.card)
        val advanceTurnMode = cardsOnTable.all { (_, v) -> v.isNotEmpty() }
        val beginner = (state.lastRoundBeginner + 1) % state.numPlayers

        val newState = state.copy(
            currentTurnMode = if (advanceTurnMode) TurnMode.LAY else state.currentTurnMode,
            currentTurn = if (advanceTurnMode) nextValidTurn(beginner, state.numPlayers, state.cardsAvailable) else state.currentTurn,
            lastRoundBeginner = if (advanceTurnMode) beginner else state.lastRoundBeginner,
            cardsInHand = cardsInHand,
            cardsOnTable = cardsOnTable
        )

        return newState to Continue(newState.currentTurn)
    }
}
