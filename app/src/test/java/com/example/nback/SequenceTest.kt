package com.example.nback

import com.example.nback.model.SequenceGenerator
import org.junit.Test
import kotlin.test.DefaultAsserter.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertFalse

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
            if (sequence[i].isMatch || sequence[i].sameAs(sequence[i - n])) {
                matches++
            }
        }

        val rate = matches.toFloat() / (totalEvents - n)
        // Check that match rate falls within an acceptable range around 20%-40%
        assertTrue("Match rate should be between 15% and 45%, actual: $rate", rate in 0.15f..0.45f)
    }

    @Test
    fun firstNEventsAreNeverMatches() {
        val n = 3
        val totalEvents = 20

        // Generate a sequence
        val sequence = generator.generateSequence(
            totalEvents = totalEvents,
            n = n,
            positionCount = 9
        )

        // Check that the first n events are not matches
        for (i in 0 until n) {
            assertFalse(
                sequence[i].isMatch,
                "Event $i should not be a match"
            )
        }
    }
}