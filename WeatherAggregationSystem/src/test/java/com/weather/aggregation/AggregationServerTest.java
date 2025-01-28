package com.weather.aggregation;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.concurrent.*;

public class AggregationServerTest {

    private static AggregationServer server;
    private static Thread serverThread;

    @TempDir
    File tempDir;

    @BeforeAll
    public static void startServer() throws Exception {
        serverThread = new Thread(() -> {
            try {
                server = new AggregationServer(4567);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        serverThread.start();
        // Wait for server to start
        Thread.sleep(2000);
    }

    @AfterAll
    public static void stopServer() {
        if (server != null) {
            server.shutdown();
        }
    }

    @Test
    public void testConcurrentPUTAndGETRequests() throws InterruptedException {
        // Initialize ContentServers and GETClients
        ContentServer contentServer1 = new ContentServer();
        ContentServer contentServer2 = new ContentServer();
        GETClient client1 = new GETClient();
        GETClient client2 = new GETClient();

        // Prepare test data files
        File testDataFile1 = new File(tempDir, "test_weather_data1.txt");
        File testDataFile2 = new File(tempDir, "test_weather_data2.txt");
        createTestDataFile(testDataFile1.getAbsolutePath(), "station1", 20.0);
        createTestDataFile(testDataFile2.getAbsolutePath(), "station2", 22.5);

        // Start PUT requests in separate threads
        Thread putThread1 = new Thread(() -> contentServer1.sendPUT("http://localhost:4567", testDataFile1.getAbsolutePath()));
        Thread putThread2 = new Thread(() -> contentServer2.sendPUT("http://localhost:4567", testDataFile2.getAbsolutePath()));
        putThread1.start();
        putThread2.start();
        putThread1.join();
        putThread2.join();

        // Start GET requests in separate threads
        Thread getThread1 = new Thread(() -> client1.sendGET("http://localhost:4567", "station1"));
        Thread getThread2 = new Thread(() -> client2.sendGET("http://localhost:4567", "station2"));
        getThread1.start();
        getThread2.start();
        getThread1.join();
        getThread2.join();

        // Note: Add assertions or output verification if necessary
    }

    private void createTestDataFile(String filePath, String stationId, double temp) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.println("id:" + stationId);
            writer.println("temp:" + temp);
            writer.println("lamportClock:1");
        } catch (IOException e) {
            fail("Failed to write test data file: " + e.getMessage());
        }
    }
}
