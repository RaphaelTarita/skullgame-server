package com.rtarita.skull.server.core.game

import com.rtarita.skull.common.moves.Bid
import com.rtarita.skull.common.moves.FirstCard
import com.rtarita.skull.common.moves.Guess
import com.rtarita.skull.common.moves.Lay
import com.rtarita.skull.common.moves.Move
import com.rtarita.skull.common.moves.MoveOutcome
import com.rtarita.skull.common.state.GameState
import com.rtarita.skull.common.state.PlayerGameState
import com.rtarita.skull.server.core.game.handler.BidHandler
import com.rtarita.skull.server.core.game.handler.FirstCardHandler
import com.rtarita.skull.server.core.game.handler.GuessHandler
import com.rtarita.skull.server.core.game.handler.LayHandler
import com.rtarita.skull.server.core.game.handler.MoveHandler
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

    suspend fun tickGame(playerIndex: Int, move: Move) = when (move) {
        is FirstCard -> handleMove(FirstCardHandler, playerIndex, move)
        is Lay -> handleMove(LayHandler, playerIndex, move)
        is Bid -> handleMove(BidHandler, playerIndex, move)
        is Guess -> handleMove(GuessHandler, playerIndex, move)
    }

    private suspend fun <T : Move> handleMove(handler: MoveHandler<T>, playerIndex: Int, move: T): MoveOutcome = holder.modifyState { state ->
        val checkResult = handler.check(state, playerIndex, move)
        if (checkResult != null) {
            return checkResult
        } else {
            handler.handle(state, playerIndex, move, random)
        }
    }
}
