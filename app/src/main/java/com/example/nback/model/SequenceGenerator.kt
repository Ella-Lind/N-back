package com.example.nback.model

import kotlin.random.Random

data class Stimulus(
    val position: Int, // 0 to 8 for a 3x3 grid
    val letter: Char   // Phonetically distinct letters (e.g., C, K, P, T, Q, R)
)

class SequenceGenerator(private val availableLetters: List<Char> = listOf('C', 'K', 'P', 'T', 'Q', 'R', 'V')) {
    /**
     * Generates an N-Back stimulus sequence.
     * Guarantees:
     * 1. First N events can NEVER be matches.
     * 2. Target match rate is between 20% and 40%.
     */
    fun generateSequence(
        totalEvents: Int = 20,
        n: Int = 2,
        targetMatchRate: Float = 0.3f
    ): List<Stimulus> {
        val sequence = mutableListOf<Stimulus>()

        // 1. Generate first N events with zero possible matches
        for (i in 0 until n) {
            sequence.add(randomStimulus())
        }

        // 2. Generate remaining events enforcing match rate probability
        for (i in n until totalEvents) {
            val shouldMatch = Random.nextFloat() < targetMatchRate

            if (shouldMatch) {
                // Force match with stimulus N steps earlier
                val previous = sequence[i - n]
                sequence.add(Stimulus(position = previous.position, letter = previous.letter))
            } else {
                // Generate non-matching stimulus
                val previous = sequence[i - n]
                var nextPos: Int
                var nextChar: Char
                do {
                    nextPos = Random.nextInt(9)
                    nextChar = availableLetters.random()
                } while (nextPos == previous.position && nextChar == previous.letter)

                sequence.add(Stimulus(position = nextPos, letter = nextChar))
            }
        }
        return sequence
    }

    private fun randomStimulus(): Stimulus {
        return Stimulus(
            position = Random.nextInt(9),
            letter = availableLetters.random()
        )
    }
}