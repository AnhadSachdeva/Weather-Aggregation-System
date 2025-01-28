package com.weather.aggregation;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

/**
 * Singleton class that manages weather data storage with thread-safe operations.
 */
public class DataStore {
    private static DataStore instance = null;
    private final String filePath = "data/weather_data.json";
    private Map<String, Map<String, Object>> weatherData;
    private Map<String, Long> lastUpdateTime;
    private final SimpleJsonParser jsonParser;
    private final ReadWriteLock lock;

    private DataStore() {
        jsonParser = new SimpleJsonParser();
        lock = new ReentrantReadWriteLock();
        weatherData = new HashMap<>();
        lastUpdateTime = new HashMap<>();
        loadData();
        startExpirationTask();
    }

    /**
     * Gets the singleton instance of the DataStore.
     *
     * @return The singleton DataStore instance.
     */
    public static synchronized DataStore getInstance() {
        if (instance == null) {
            instance = new DataStore();
        }
        return instance;
    }

    // Load data from file
    private void loadData() {
        lock.writeLock().lock();
        try {
            File file = new File(filePath);
            if (file.exists()) {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String jsonString = sb.toString();
                try {
                    Map<String, Object> parsedData = jsonParser.parse(jsonString);
                    weatherData = (Map<String, Map<String, Object>>) (Map) parsedData;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // Initialize lastUpdateTime based on existing data
                for (String id : weatherData.keySet()) {
                    lastUpdateTime.put(id, System.currentTimeMillis());
                }
            } else {
                weatherData = new HashMap<>();
                lastUpdateTime = new HashMap<>();
                // Ensure the data directory exists
                File dataDir = new File("data");
                if (!dataDir.exists()) {
                    dataDir.mkdirs();
                }
                saveData(); // Create the file
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    // Save data to file atomically
    private void saveData() {
        lock.readLock().lock();
        try {
            File tempFile = new File(filePath + ".tmp");
            String jsonString = jsonParser.toJson((Map<String, Object>) (Map) weatherData);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
                writer.write(jsonString);
            }
            File actualFile = new File(filePath);
            if (!tempFile.renameTo(actualFile)) {
                throw new IOException("Failed to rename temp file to actual data file.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Adds or updates weather data.
     *
     * @param data The weather data to store.
     * @return True if the data is new, false if it was updated.
     */
    public boolean putData(Map<String, Object> data) {
        lock.writeLock().lock();
        try {
            String id = (String) data.get("id");
            if (id == null) {
                return false;
            }
            boolean isNew = !weatherData.containsKey(id);
            weatherData.put(id, data);
            lastUpdateTime.put(id, System.currentTimeMillis());
            saveData();
            return isNew;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Retrieves the current weather data.
     *
     * @return A map of station IDs to their weather data.
     */
    public Map<String, Map<String, Object>> getData() {
        lock.readLock().lock();
        try {
            return new HashMap<>(weatherData);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Expires data not updated within 30 seconds.
     */
    public void expireData() {
        lock.writeLock().lock();
        try {
            long currentTime = System.currentTimeMillis();
            Iterator<String> iterator = lastUpdateTime.keySet().iterator();
            while (iterator.hasNext()) {
                String id = iterator.next();
                long lastTime = lastUpdateTime.get(id);
                if (currentTime - lastTime > 30000) { // 30 seconds
                    iterator.remove();
                    weatherData.remove(id);
                    System.out.println("Expired data for ID: " + id);
                }
            }
            saveData();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.writeLock().unlock();
        }
    }

    // Start a scheduled task to expire data every 10 seconds
    private void startExpirationTask() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::expireData, 10, 10, TimeUnit.SECONDS);
    }

    /**
     * Clears all data from the DataStore (for testing purposes).
     */
    public void clearData() {
        lock.writeLock().lock();
        try {
            weatherData.clear();
            lastUpdateTime.clear();
            saveData();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.writeLock().unlock();
        }
    }
}
