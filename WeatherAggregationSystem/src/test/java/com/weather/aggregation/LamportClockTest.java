package com.weather.aggregation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class LamportClockTest {

    @Test
    public void testInitialClockValue() {
        LamportClock clock = new LamportClock();
        assertEquals(0, clock.getTime(), "Initial clock value should be 0");
    }

    @Test
    public void testTick() {
        LamportClock clock = new LamportClock();
        clock.tick();
        assertEquals(1, clock.getTime(), "Clock value should be 1 after one tick");
    }

    @Test
    public void testUpdate() {
        LamportClock clock = new LamportClock();
        clock.update(5);
        assertEquals(6, clock.getTime(), "Clock should update to max(receivedTime, localTime) + 1");
    }

    @Test
    public void testUpdateWithLowerValue() {
        LamportClock clock = new LamportClock();
        clock.tick(); // clock = 1
        clock.update(0);
        assertEquals(2, clock.getTime(), "Clock should increment after update with lower received time");
    }
}
