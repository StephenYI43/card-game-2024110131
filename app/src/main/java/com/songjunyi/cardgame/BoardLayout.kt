package com.songjunyi.cardgame

/** Pure policy: orientation affects new rounds; rotating preserves the current round. */
object BoardLayout {
    fun newRoundCount(landscape: Boolean) = if (landscape) 36 else 24
    fun columns(landscape: Boolean) = if (landscape) 9 else 4
}
