package com.nowshowing.tv.ui.player

internal object PointerMotion {
    /** Short taps are precise; a held direction accelerates without becoming uncontrollable. */
    fun stepDp(repeatCount: Int): Float = when {
        repeatCount < 4 -> 8f
        repeatCount < 12 -> 16f
        else -> 28f
    }
}
