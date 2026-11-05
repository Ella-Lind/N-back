package com.example.nback.model

import kotlin.random.Random

/**
 * One event of a round: a grid cell that lights up and a letter that is spoken at the same time.
 *
 * @property position grid cell index, 0 until positionCount (0 to 8 for a 3x3 grid).
 * @property letter the spoken letter (phonetically distinct letters, e.g. C, K, P, T, Q, R).
 * @property isMatch true if this event repeats the stimulus from `n` events earlier. Set by
 *   [SequenceGenerator]; always false for the first `n` events.
 */
data class Stimulus(
    val position: Int,
    val letter: Char,
    val isMatch: Boolean = false
) {
    /** True if [other] has the same position AND the same letter (ignores [isMatch]). */
    fun sameAs(other: Stimulus): Boolean = position == other.position && letter == other.letter
}

/**
 * Generates the stimulus sequence for one N-back round.
 *
 * Guarantees:
 *  1. The first `n` events are never matches.
 *  2. The number of matches is between [MIN_MATCH_PERCENT] and [MAX_MATCH_PERCENT] percent of the
 *     events that can match (everything after the first `n`). The count is decided first, so the
 *     rate is exact instead of depending on luck.
 *  3. An event that is not planned as a match never matches by accident.
 *
 * @param availableLetters the pool of letters that can be spoken.
 * @param random source of randomness. Pass a seeded [Random] in tests for reproducible results.
 */
class SequenceGenerator(
    private val availableLetters: List<Char> = listOf('C', 'K', 'P', 'T', 'Q', 'R', 'V'),
    private val random: Random = Random.Default
) {

    /**
     * Generates a sequence.
     *
     * Plan first, fill second: (1) pick how many matches there will be and which slots they are,
     * (2) walk through the sequence. A match slot copies the stimulus from `n` steps back; every
     * other slot is drawn randomly but redrawn if it would equal the stimulus `n` steps back.
     *
     * @param totalEvents number of events in the round.
     * @param n how many events back a match refers to, at least 1.
     * @param positionCount number of grid cells (9 for a 3x3 grid).
     * @throws IllegalArgumentException if the arguments cannot produce a valid round, for example
     *   a round too short to hold any match.
     */
    fun generateSequence(
        totalEvents: Int = 20,
        n: Int = 2,
        positionCount: Int = 9,
        targetMatchRate: Float? = null
    ): List<Stimulus> {
        require(n >= 1) { "n must be at least 1 (was $n)" }
        require(totalEvents > n) { "totalEvents ($totalEvents) must be larger than n ($n)" }
        require(positionCount * availableLetters.size >= 2) {
            "need at least two different possible stimuli"
        }

        // Step 1: plan. Only slots from index n onward can be matches, which is what guarantees
        // that the first n events never match.
        val eligible = totalEvents - n
        val matchCount = if (targetMatchRate != null) {
            val range = matchCountRange(totalEvents, n)
            val calculated = kotlin.math.round(eligible * targetMatchRate).toInt()
            calculated.coerceIn(range.first, range.last)
        } else {
            val range = matchCountRange(totalEvents, n)
            random.nextInt(range.first, range.last + 1)
        }
        val matchSlots = (n until totalEvents).shuffled(random).take(matchCount).toSet()

        // Step 2: fill.
        val sequence = ArrayList<Stimulus>(totalEvents)
        for (i in 0 until totalEvents) {
            val stimulus = when {
                i in matchSlots -> {
                    val earlier = sequence[i - n]
                    Stimulus(earlier.position, earlier.letter, isMatch = true)
                }
                i < n -> randomStimulus(positionCount)
                else -> randomStimulusDifferentFrom(sequence[i - n], positionCount)
            }
            sequence.add(stimulus)
        }
        return sequence
    }

    /** A random stimulus (never flagged as a match). */
    private fun randomStimulus(positionCount: Int): Stimulus =
        Stimulus(position = random.nextInt(positionCount), letter = availableLetters.random(random))

    /**
     * A random stimulus that is NOT identical (same position and same letter) to [earlier].
     * Redraws until it differs; this ends quickly because almost every draw already differs.
     */
    private fun randomStimulusDifferentFrom(earlier: Stimulus, positionCount: Int): Stimulus {
        var candidate: Stimulus
        do {
            candidate = randomStimulus(positionCount)
        } while (candidate.sameAs(earlier))
        return candidate
    }

    companion object {
        /** Lowest allowed share of matches, in percent. */
        const val MIN_MATCH_PERCENT = 20

        /** Highest allowed share of matches, in percent. */
        const val MAX_MATCH_PERCENT = 40

        /**
         * Allowed number of matches for a round.
         *
         * The percentages apply to the events that can match (`totalEvents - n`). The lower bound
         * is also raised to [MIN_MATCH_PERCENT] of the whole round when that still fits under the
         * upper bound, so the rate looks right if it is measured over all events too. Integer
         * arithmetic is used to avoid floating point rounding surprises.
         *
         * @throws IllegalArgumentException if no whole number of matches fits the percentages.
         */
        internal fun matchCountRange(totalEvents: Int, n: Int): IntRange {
            val eligible = totalEvents - n
            val upper = eligible * MAX_MATCH_PERCENT / 100
            val wholeRoundLower = ceilPercent(totalEvents, MIN_MATCH_PERCENT)
            val lower =
                if (wholeRoundLower <= upper) wholeRoundLower else ceilPercent(eligible, MIN_MATCH_PERCENT)
            require(lower <= upper) {
                "Round too short: $eligible possible match slots cannot hold " +
                    "$MIN_MATCH_PERCENT-$MAX_MATCH_PERCENT% matches"
            }
            return lower..upper
        }

        /** [percent] percent of [value], rounded up, using integer arithmetic only. */
        private fun ceilPercent(value: Int, percent: Int): Int = (value * percent + 99) / 100
    }
}
