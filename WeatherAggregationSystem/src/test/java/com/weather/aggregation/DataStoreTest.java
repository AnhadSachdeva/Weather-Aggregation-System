package com.weather.aggregation;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class DataStoreTest {

    private DataStore dataStore;

    @BeforeEach
    public void setUp() {
        dataStore = DataStore.getInstance();
        dataStore.clearData(); // Clear existing data for test isolation
    }

    @Test
    public void testPutData() {
        Map<String, Object> data = new HashMap<>();
        data.put("id", "testStation");
        data.put("temp", 25.0);

        boolean isNew = dataStore.putData(data);
        assertTrue(isNew, "Data should be new on first insert");
        assertNotNull(dataStore.getData().get("testStation"), "Data should be retrievable after insert");
    }

    @Test
    public void testPutDataUpdate() {
        Map<String, Object> data = new HashMap<>();
        data.put("id", "testStation");
        data.put("temp", 25.0);
        dataStore.putData(data);

        data.put("temp", 30.0);
        boolean isNew = dataStore.putData(data);
        assertFalse(isNew, "Data should not be new on update");
        assertEquals(30.0, dataStore.getData().get("testStation").get("temp"), "Data should be updated");
    }

    @Test
    public void testExpireData() throws InterruptedException {
        Map<String, Object> data = new HashMap<>();
        data.put("id", "testStation");
        data.put("temp", 25.0);
        dataStore.putData(data);

        // Wait for 31 seconds to ensure data expires
        Thread.sleep(31000);

        dataStore.expireData(); // Manually trigger expiration

        assertNull(dataStore.getData().get("testStation"), "Data should be expired and removed");
    }

    @Test
    public void testConcurrentDataAccess() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", "station" + index);
                    data.put("temp", 20.0 + index);
                    boolean isNew = dataStore.putData(data);
                    assertTrue(isNew, "Data should be new on first insert for station" + index);

                    Map<String, Object> retrievedData = dataStore.getData().get("station" + index);
                    assertNotNull(retrievedData, "Data should be retrievable for station" + index);
                    assertEquals(20.0 + index, retrievedData.get("temp"), "Temperature should match for station" + index);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();
    }
}
