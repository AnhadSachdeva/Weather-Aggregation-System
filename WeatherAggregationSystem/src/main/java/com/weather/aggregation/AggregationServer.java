package com.weather.aggregation;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main server class that aggregates weather data from content servers.
 */
public class AggregationServer {
    private static final Logger logger = LoggerFactory.getLogger(AggregationServer.class);
    private ServerSocket serverSocket;
    private ExecutorService executor;
    private LamportClock lamportClock;
    private DataStore dataStore;

    /**
     * Initializes the Aggregation Server on the specified port.
     *
     * @param port The port number to listen on.
     * @throws IOException If an I/O error occurs when opening the socket.
     */
    public AggregationServer(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        executor = Executors.newCachedThreadPool();
        lamportClock = new LamportClock();
        dataStore = DataStore.getInstance();

        logger.info("Aggregation Server started on port {}", port);

        // Start accepting client connections
        acceptConnections();
    }

    /**
     * Accepts client connections and handles them using a thread pool.
     */
    private void acceptConnections() {
        executor.submit(() -> {
            while (!serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    executor.submit(new ClientHandler(clientSocket, lamportClock, dataStore));
                } catch (IOException e) {
                    if (serverSocket.isClosed()) {
                        logger.info("Server socket closed.");
                    } else {
                        logger.error("Error accepting client connection", e);
                    }
                }
            }
        });
    }

    /**
     * Shuts down the server and releases resources.
     */
    public void shutdown() {
        try {
            serverSocket.close();
            executor.shutdown();
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
            logger.info("Aggregation Server shut down.");
        } catch (IOException | InterruptedException e) {
            logger.error("Error shutting down server", e);
        }
    }

    /**
     * Main method to start the Aggregation Server.
     *
     * @param args Command-line arguments (port number).
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java -cp target/WeatherAggregationSystem-1.0-SNAPSHOT-shaded.jar com.weather.aggregation.AggregationServer <port_number>");
            return;
        }

        int port = Integer.parseInt(args[0]);

        try {
            AggregationServer server = new AggregationServer(port);
            // Keep the main thread alive
            Thread.currentThread().join();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
