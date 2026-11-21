package com.rami.network.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ChannelNumberingTest {

    @Test
    fun firstChannelGetsNumberOne() {
        assertEquals(1, ChannelNumbering.nextNumber(emptyList()))
    }

    @Test
    fun nextNumberFollowsHighestUsedAndDoesNotReuseGaps() {
        assertEquals(4, ChannelNumbering.nextNumber(listOf(1, 3)))
    }

    @Test
    fun ignoresInvalidHistoricalNumbers() {
        assertEquals(2, ChannelNumbering.nextNumber(listOf(-5, 0, 1)))
    }

    @Test
    fun failsClearlyWhenNumberSpaceIsExhausted() {
        assertThrows(ArithmeticException::class.java) {
            ChannelNumbering.nextNumber(listOf(Int.MAX_VALUE))
        }
    }
}
