package com.rtarita.skull.server.core.game.handler

import com.rtarita.skull.common.CommonConstants
import com.rtarita.skull.common.moves.BadMove
import com.rtarita.skull.common.moves.Continue
import com.rtarita.skull.common.moves.GameEnded
import com.rtarita.skull.common.moves.Guess
import com.rtarita.skull.common.moves.MoveOutcome
import com.rtarita.skull.common.moves.RoundEnded
import com.rtarita.skull.common.moves.RoundOutcome
import com.rtarita.skull.common.state.Card
import com.rtarita.skull.common.state.GameState
import com.rtarita.skull.common.state.TurnMode
import com.rtarita.skull.server.util.mutate
import kotlin.random.Random

object GuessHandler : MoveHandler<Guess> {
    override fun check(state: GameState, playerIndex: Int, move: Guess): BadMove? {
        if (state.currentTurnMode != TurnMode.GUESS) {
            return BadMove("cannot guess, game is in ${state.currentTurnMode} mode")
        }

        if (state.currentTurn != playerIndex) {
            return BadMove("cannot guess, it is not player's turn")
        }

        if (state.cardsOnTable[move.otherPlayerIndex]?.get(move.cardIndex) == null) {
            return BadMove("cannot guess, player '${move.otherPlayerIndex}' inexistent or has not laid card no. ${move.cardIndex}")
        }

        if ((move.otherPlayerIndex to move.cardIndex) in state.revealedCards) {
            return BadMove("cannot guess, card is already revealed")
        }

        val ownCardsRevealed = state.revealedCards.count { (pIdx, _) -> pIdx == playerIndex }
        if (move.otherPlayerIndex != playerIndex && ownCardsRevealed < state.cardsOnTable.getValue(playerIndex).size) {
            return BadMove("cannot guess, player must reveal own cards first")
        }
        return null
    }

    override fun handle(state: GameState, playerIndex: Int, move: Guess, random: Random): Pair<GameState, MoveOutcome> {
        val newRevealedCards = state.revealedCards + (move.otherPlayerIndex to move.cardIndex)
        return when (state.cardsOnTable.getValue(move.otherPlayerIndex)[move.cardIndex]) {
            Card.ROSE -> {
                val ended = newRevealedCards.size >= state.bids.getValue(playerIndex)
                val newPoints = state.points.mutate(playerIndex) { (_, v) -> v + 1 }
                val won = newPoints.getValue(playerIndex) >= CommonConstants.WIN_WITH_POINTS

                val newState = if (ended) resetRound(state, newPoints = newPoints) else state.copy(revealedCards = newRevealedCards)
                val outcome = when {
                    ended && won -> GameEnded(playerIndex)
                    ended && !won -> RoundEnded(RoundOutcome.WON, playerIndex)
                    else -> Continue(playerIndex)
                }
                newState to outcome
            }


            Card.SKULL -> {
                val newCardsAvailable = state.cardsAvailable.mutate(playerIndex) { (_, v) -> v - v.random(random) }

                val potentialWinner = newCardsAvailable
                    .entries
                    .singleOrNull { (_, v) -> v.isNotEmpty() }
                    ?.key

                val newState = resetRound(state, newCardsAvailable = newCardsAvailable)
                val outcome = if (potentialWinner != null) {
                    GameEnded(potentialWinner)
                } else {
                    RoundEnded(RoundOutcome.LOST, playerIndex)
                }
                newState to outcome
            }
        }
    }
}
