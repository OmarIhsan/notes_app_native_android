package com.mal5odha.core.ink.laser

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Ephemeral zero-allocation circular ring buffer for high-frequency laser pointer touch and hover events.
 *
 * Backed entirely by primitive flat arrays (`FloatArray`, `LongArray`) to ensure zero heap object allocation
 * and zero GC churn during 120Hz/144Hz inking sweeps.
 *
 * Thread-safe for concurrent event ingestion and rendering loop consumption.
 */
class LaserTrailBuffer(val capacity: Int = 1024) {
    init {
        require(capacity > 0) { "Capacity must be greater than 0, given $capacity" }
    }

    @PublishedApi internal val xs = FloatArray(capacity)
    @PublishedApi internal val ys = FloatArray(capacity)
    @PublishedApi internal val timestamps = LongArray(capacity)
    @PublishedApi internal val pressures = FloatArray(capacity)

    @PublishedApi internal val lock = ReentrantLock()

    @Volatile
    @PublishedApi internal var head = 0 // Next insertion index

    @Volatile
    @PublishedApi internal var tail = 0 // Oldest active index

    @Volatile
    @PublishedApi internal var count = 0

    val size: Int
        get() = lock.withLock { count }

    fun isEmpty(): Boolean = lock.withLock { count == 0 }

    fun isNotEmpty(): Boolean = lock.withLock { count > 0 }

    /**
     * Appends a new point to the ring buffer.
     * If the buffer is full, the oldest point is overwritten in O(1) time.
     */
    fun push(x: Float, y: Float, timestamp: Long, pressure: Float = 1.0f) {
        lock.withLock {
            if (count == capacity) {
                // Buffer is full; overwrite the oldest entry and advance tail
                tail = (tail + 1) % capacity
            } else {
                count++
            }
            xs[head] = x
            ys[head] = y
            timestamps[head] = timestamp
            pressures[head] = pressure
            head = (head + 1) % capacity
        }
    }

    /**
     * Discards points older than [ttlMs] milliseconds relative to [now].
     * Implements progressive comet-tail decay on a rolling window.
     */
    fun pruneExpired(now: Long, ttlMs: Long) {
        lock.withLock {
            while (count > 0) {
                val age = now - timestamps[tail]
                if (age > ttlMs) {
                    tail = (tail + 1) % capacity
                    count--
                } else {
                    break
                }
            }
        }
    }

    /**
     * Resets the ring buffer pointers in O(1) without reallocating arrays.
     */
    fun clear() {
        lock.withLock {
            head = 0
            tail = 0
            count = 0
        }
    }

    /**
     * Returns the X coordinate at [logicalIndex] (0 = oldest, count - 1 = newest).
     */
    fun getX(logicalIndex: Int): Float = lock.withLock {
        checkBounds(logicalIndex)
        xs[(tail + logicalIndex) % capacity]
    }

    /**
     * Returns the Y coordinate at [logicalIndex] (0 = oldest, count - 1 = newest).
     */
    fun getY(logicalIndex: Int): Float = lock.withLock {
        checkBounds(logicalIndex)
        ys[(tail + logicalIndex) % capacity]
    }

    /**
     * Returns the timestamp at [logicalIndex].
     */
    fun getTimestamp(logicalIndex: Int): Long = lock.withLock {
        checkBounds(logicalIndex)
        timestamps[(tail + logicalIndex) % capacity]
    }

    /**
     * Returns the pressure at [logicalIndex].
     */
    fun getPressure(logicalIndex: Int): Float = lock.withLock {
        checkBounds(logicalIndex)
        pressures[(tail + logicalIndex) % capacity]
    }

    private fun checkBounds(index: Int) {
        if (index !in 0 until count) {
            throw IndexOutOfBoundsException("Index $index out of bounds for size $count")
        }
    }

    /**
     * Copies active points to caller-provided primitive arrays in a single locked pass.
     * Prevents allocation during steady-state canvas drawing.
     *
     * @return Number of points copied.
     */
    fun copyTo(
        outXs: FloatArray,
        outYs: FloatArray,
        outTimestamps: LongArray,
        outPressures: FloatArray,
        maxPoints: Int = outXs.size
    ): Int {
        lock.withLock {
            val toCopy = minOf(count, maxPoints, outYs.size, outTimestamps.size, outPressures.size)
            var curr = tail
            for (i in 0 until toCopy) {
                outXs[i] = xs[curr]
                outYs[i] = ys[curr]
                outTimestamps[i] = timestamps[curr]
                outPressures[i] = pressures[curr]
                curr = (curr + 1) % capacity
            }
            return toCopy
        }
    }

    /**
     * Writes the latest point coordinates into [out] (FloatArray of size >= 2).
     * @return true if a point was written, false if buffer is empty.
     */
    fun getLatestPoint(out: FloatArray): Boolean {
        require(out.size >= 2) { "Output array must have length >= 2" }
        lock.withLock {
            if (count == 0) return false
            val lastIdx = if (head == 0) capacity - 1 else head - 1
            out[0] = xs[lastIdx]
            out[1] = ys[lastIdx]
            return true
        }
    }
}
