package com.rami.network.domain

/**
 * Local channel-number helper.
 *
 * The backend MUST allocate numbers inside a database transaction with a unique
 * constraint on channel.number. This helper is not safe for concurrent allocation.
 */
object ChannelNumbering {

    /**
     * Returns one greater than the highest positive number ever retained in the
     * supplied collection. Deleted numbers are not reused when their historical
     * allocation is retained by the backend.
     *
     * Throws ArithmeticException if Int capacity is exhausted.
     */
    fun nextNumber(existingNumbers: Collection<Int>): Int {
        val highest = existingNumbers.filter { it > 0 }.maxOrNull() ?: 0
        return Math.addExact(highest, 1)
    }
}
