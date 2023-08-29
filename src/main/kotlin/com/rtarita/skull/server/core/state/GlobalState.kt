package com.rtarita.skull.server.core.state

import com.rtarita.skull.server.core.game.GameStore

internal object GlobalState {
    var active: Boolean = false

    val gameStore = GameStore()
}
