package com.weather.aggregation;

/**
 * Implements Lamport's logical clock for synchronization in distributed systems.
 */
public class LamportClock {
    private int clock;

    /**
     * Initializes the Lamport clock to zero.
     */
    public LamportClock() {
        this.clock = 0;
    }

    /**
     * Increments the clock to represent a local event.
     */
    public synchronized void tick() {
        clock++;
    }

    /**
     * Updates the clock based on a received clock value.
     *
     * @param receivedClock The Lamport clock value received from another process.
     */
    public synchronized void update(int receivedClock) {
        clock = Math.max(clock, receivedClock) + 1;
    }

    /**
     * Retrieves the current clock value.
     *
     * @return The current Lamport clock value.
     */
    public synchronized int getTime() {
        return clock;
    }

    /**
     * Sets the clock to a specific value.
     *
     * @param time The value to set the Lamport clock to.
     */
    public synchronized void setTime(int time) {
        this.clock = time;
    }
}
