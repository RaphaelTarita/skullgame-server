package com.rtarita.skull.server.core.game.handler

import com.rtarita.skull.common.Card
import com.rtarita.skull.common.GameState
import com.rtarita.skull.server.util.mutate

internal fun layCard(
    cardsInHand: Map<Int, List<Card>>,
    cardsOnTable: Map<Int, List<Card>>,
    pIdx: Int,
    card: Card
): Pair<Map<Int, List<Card>>, Map<Int, List<Card>>> =
    cardsInHand.mutate(pIdx) { (_, v) -> v - card } to cardsOnTable.mutate(pIdx) { (_, v) -> v + card }

internal fun nextValidTurn(
    current: Int,
    numPlayers: Int,
    cardsAvailable: Map<Int, List<Card>>
) = if (cardsAvailable.getValue(current).isEmpty()) {
    advanceTurn(current, numPlayers, cardsAvailable)
} else {
    current
}

internal fun advanceTurn(
    current: Int,
    numPlayers: Int,
    cardsAvailable: Map<Int, List<Card>>
) = turnOrder(current, numPlayers).first {
    cardsAvailable.getValue(it).isNotEmpty()
}

private fun turnOrder(beginningFrom: Int, numPlayers: Int) = (beginningFrom + 1 until numPlayers) + (0..beginningFrom)

internal fun resetRound(
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
