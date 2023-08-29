package com.rtarita.skull.server.core.game.handler

import com.rtarita.skull.common.BadMove
import com.rtarita.skull.common.Bid
import com.rtarita.skull.common.Continue
import com.rtarita.skull.common.GameState
import com.rtarita.skull.common.MoveOutcome
import com.rtarita.skull.common.TurnMode
import kotlin.random.Random

object BidHandler : MoveHandler<Bid> {
    override fun check(state: GameState, playerIndex: Int, move: Bid): BadMove? {
        if (state.currentTurnMode != TurnMode.LAY && state.currentTurnMode != TurnMode.BID) {
            return BadMove("cannot bid, game is in ${state.currentTurnMode} mode")
        }

        if (state.currentTurn != playerIndex) {
            return BadMove("cannot bid, it is not player's turn")
        }

        if (move.bid != -1 && move.bid <= state.bids.values.max()) {
            return BadMove("cannot bid ${move.bid} as it is equal or lower than the current highest bid")
        }

        if (state.bids.getValue(playerIndex) == -1 && move.bid != -1) {
            return BadMove("cannot bid anymore because player already passed")
        }
        return null
    }

    override fun handle(state: GameState, playerIndex: Int, move: Bid, random: Random): Pair<GameState, MoveOutcome> {
        val newBids = state.bids + (playerIndex to move.bid)

        val potentialGuesser = if (move.bid >= state.cardsOnTable.values.sumOf { it.size }) {
            playerIndex
        } else {
            newBids.entries
                .singleOrNull { (_, v) -> v > -1 }
                ?.key
        }

        val newState = state.copy(
            currentTurnMode = if (potentialGuesser != null) TurnMode.GUESS else TurnMode.BID,
            currentTurn = potentialGuesser ?: advanceTurn(state.currentTurn, state.numPlayers, state.cardsAvailable),
            bids = newBids
        )

        return newState to Continue(newState.currentTurn)
    }
}
