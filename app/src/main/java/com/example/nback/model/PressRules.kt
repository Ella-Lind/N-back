package com.example.nback.model

/**
 * The rule for when a press of the MATCH button counts.
 *
 * Kept as plain Kotlin with no Android or coroutine code so it can be unit tested directly.
 * Both the ViewModel (to decide whether to count a press) and the screen (to decide whether the
 * button looks enabled) use this one function, so they can never disagree.
 */
object PressRules {

    /**
     * A press counts only if all of these hold:
     *  - the round is running (so presses before the round starts and after it ends are ignored),
     *  - the current event is at index [n] or later (the first `n` events have nothing to
     *    compare against), and
     *  - the player has not already pressed during this event (double-press guard).
     *
     * @param isRunning true while a round is in progress.
     * @param currentIndex index of the event currently shown (-1 before the first event).
     * @param n how many events back a match refers to.
     * @param alreadyPressedThisTick true if a press was already counted for the current event.
     */
    fun isPressAllowed(
        isRunning: Boolean,
        currentIndex: Int,
        n: Int,
        alreadyPressedThisTick: Boolean
    ): Boolean = isRunning && currentIndex >= n && !alreadyPressedThisTick
}
