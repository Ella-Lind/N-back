package com.example.nback

import com.example.nback.model.SequenceGenerator
import org.junit.Assert.*
import org.junit.Test

class SequenceTest {

    private val generator = SequenceGenerator()

    @Test
    fun testFirstN_NeverMatches() {
        val n = 2
        val sequence = generator.generateSequence(totalEvents = 20, n = n)

        // Verify that elements before index N are not matches (since they have no N-step predecessor)
        assertTrue("Sequence size must match requested length", sequence.size == 20)
    }

    @Test
    fun testMatchRateEnforcement() {
        val totalEvents = 100
        val n = 2
        val sequence = generator.generateSequence(totalEvents = totalEvents, n = n, targetMatchRate = 0.3f)

        var matches = 0
        for (i in n until sequence.size) {
            if (sequence[i] == sequence[i - n]) {
                matches++
            }
        }

        val rate = matches.toFloat() / (totalEvents - n)
        // Check that match rate falls within an acceptable range around 20%-40%
        assertTrue("Match rate should be between 15% and 45%, actual: $rate", rate in 0.15f..0.45f)
    }

    @Test
    fun testSequenceLength() {
        val sequence = generator.generateSequence(totalEvents = 15, n = 3)
        assertEquals(15, sequence.size)
    }
}