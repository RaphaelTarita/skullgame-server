package com.rtarita.skull.server.core.game

import com.rtarita.skull.common.BadMove
import com.rtarita.skull.common.Bid
import com.rtarita.skull.common.Card
import com.rtarita.skull.common.CommonConstants
import com.rtarita.skull.common.Continue
import com.rtarita.skull.common.FirstCard
import com.rtarita.skull.common.GameEnded
import com.rtarita.skull.common.GameState
import com.rtarita.skull.common.Guess
import com.rtarita.skull.common.Lay
import com.rtarita.skull.common.Move
import com.rtarita.skull.common.MoveOutcome
import com.rtarita.skull.common.PlayerGameState
import com.rtarita.skull.common.RoundEnded
import com.rtarita.skull.common.RoundOutcome
import com.rtarita.skull.common.TurnMode
import com.rtarita.skull.server.util.mutate
import java.security.SecureRandom
import kotlin.random.asKotlinRandom

internal class GameController(numPlayers: Int) {
    private val random = SecureRandom().asKotlinRandom()
    private val holder = StateHolder(GameState(numPlayers))

    fun getGameState() = holder.get()

    fun getPlayerGameState(playerIndex: Int): PlayerGameState {
        val frozen = holder.get()
        return PlayerGameState(
            playerIndex,
            frozen.numPlayers,
            frozen.roundCount,
            frozen.lastRoundBeginner,
            frozen.currentTurn,
            frozen.currentTurnMode,
            frozen.cardsAvailable.getValue(playerIndex),
            frozen.cardsAvailable.mapValues { (_, v) -> v.size },
            frozen.cardsOnTable.getValue(playerIndex),
            frozen.cardsOnTable.mapValues { (_, v) -> v.size },
            frozen.cardsInHand.getValue(playerIndex),
            frozen.cardsInHand.mapValues { (_, v) -> v.size },
            frozen.bids.toMap(),
            frozen.revealedCards.groupBy(Pair<Int, Int>::first) { (pIdx, cIdx) -> cIdx to frozen.cardsOnTable.getValue(pIdx)[cIdx] },
            frozen.points.toMap()
        )
    }

    suspend fun tickGame(playerIndex: Int, move: Move): MoveOutcome {
        return when (move) {
            is FirstCard -> handleFirstCard(playerIndex, move)
            is Lay -> handleLay(playerIndex, move)
            is Bid -> handleBid(playerIndex, move)
            is Guess -> handleGuess(playerIndex, move)
        }
    }


    private suspend fun handleFirstCard(playerIndex: Int, move: FirstCard): MoveOutcome = holder.modifyState { state ->
        if (state.currentTurnMode != TurnMode.FIRST_CARD) {
            return BadMove("cannot lay first card, game is in ${state.currentTurnMode} mode")
        }

        if (state.cardsOnTable.getValue(playerIndex).isNotEmpty()) {
            return BadMove("cannot lay first card, was already laid by player")
        }

        if (move.card !in state.cardsInHand.getValue(playerIndex)) {
            return BadMove("cannot lay first card, '${move.card}' is not in hand of player")
        }

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

        newState to Continue(newState.currentTurn)
    }

    private suspend fun handleLay(playerIndex: Int, move: Lay): MoveOutcome = holder.modifyState { state ->
        if (state.currentTurnMode != TurnMode.LAY) {
            return BadMove("cannot lay card, game is in ${state.currentTurnMode} mode")
        }

        if (state.currentTurn != playerIndex) {
            return BadMove("cannot lay card, it is not player's turn")
        }

        if (move.card !in state.cardsInHand.getValue(playerIndex)) {
            return BadMove("cannot lay card, '${move.card}' is not in hand of player")
        }

        val (cardsInHand, cardsOnTable) = layCard(state.cardsInHand, state.cardsOnTable, playerIndex, move.card)
        val newState = state.copy(
            currentTurn = advanceTurn(state.currentTurn, state.numPlayers, state.cardsAvailable),
            cardsInHand = cardsInHand,
            cardsOnTable = cardsOnTable
        )

        newState to Continue(newState.currentTurn)
    }

    private suspend fun handleBid(playerIndex: Int, move: Bid): MoveOutcome = holder.modifyState { state ->
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

        newState to Continue(newState.currentTurn)
    }

    private suspend fun handleGuess(playerIndex: Int, move: Guess): MoveOutcome = holder.modifyState { state ->
        if (state.currentTurnMode != TurnMode.GUESS) {
            return BadMove("cannot guess, game is in ${state.currentTurnMode} mode")
        }

        if (state.currentTurn != playerIndex) {
            return BadMove("cannot guess, it is not player's turn")
        }

        if (state.cardsOnTable[move.otherPlayerIndex]?.get(move.cardIndex) == null) {
            return BadMove("cannot guess, player '${move.otherPlayerIndex}' inexistent or has not laid card no. ${move.cardIndex}")
        }

        val guessPair = move.otherPlayerIndex to move.cardIndex
        if (guessPair in state.revealedCards) {
            return BadMove("cannot guess, card is already revealed")
        }

        val ownCardsRevealed = state.revealedCards.count { (pIdx, _) -> pIdx == playerIndex }
        if (move.otherPlayerIndex != playerIndex && ownCardsRevealed < state.cardsOnTable.getValue(playerIndex).size) {
            return BadMove("cannot guess, player must reveal own cards first")
        }

        val newRevealedCards = state.revealedCards + guessPair
        when (state.cardsOnTable.getValue(move.otherPlayerIndex)[move.cardIndex]) {
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

    private fun layCard(
        cardsInHand: Map<Int, List<Card>>,
        cardsOnTable: Map<Int, List<Card>>,
        pIdx: Int,
        card: Card
    ): Pair<Map<Int, List<Card>>, Map<Int, List<Card>>> =
        cardsInHand.mutate(pIdx) { (_, v) -> v - card } to cardsOnTable.mutate(pIdx) { (_, v) -> v + card }

    private fun nextValidTurn(
        current: Int,
        numPlayers: Int,
        cardsAvailable: Map<Int, List<Card>>
    ) = if (cardsAvailable.getValue(current).isEmpty()) {
        advanceTurn(current, numPlayers, cardsAvailable)
    } else {
        current
    }

    private fun advanceTurn(
        current: Int,
        numPlayers: Int,
        cardsAvailable: Map<Int, List<Card>>
    ) = turnOrder(current, numPlayers).first {
        cardsAvailable.getValue(it).isNotEmpty()
    }

    private fun turnOrder(beginningFrom: Int, numPlayers: Int) = (beginningFrom + 1 until numPlayers) + (0..beginningFrom)

    private fun resetRound(
        state: GameState,
        newPoints: Map<Int, Int> = state.points,
        newCardsAvailable: Map<Int, List<Card>> = state.cardsAvailable
    ) = GameState(
        numPlayers = state.numPlayers,
        roundCount = state.roundCount + 1,
        lastRoundBeginner = state.lastRoundBeginner,
        cardsAvailable = newCardsAvailable,
        points = newPoints
    )
}