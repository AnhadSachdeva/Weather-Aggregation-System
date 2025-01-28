package com.weather.aggregation;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.*;

/**
 * The GET Client retrieves weather data from the Aggregation Server.
 */
public class GETClient {
    private static final SimpleJsonParser jsonParser = new SimpleJsonParser();
    private LamportClock lamportClock;

    /**
     * Initializes the GET Client.
     */
    public GETClient() {
        lamportClock = new LamportClock();
    }

    /**
     * Sends a GET request to the Aggregation Server.
     *
     * @param serverUrl The URL of the Aggregation Server.
     * @param stationId The station ID to retrieve data for (optional).
     */
    public void sendGET(String serverUrl, String stationId) {
        try {
            // Parse server URL
            URL url = new URL(serverUrl);
            String host = url.getHost();
            int port = url.getPort() != -1 ? url.getPort() : 80;

            // Use the createSocket method (can be mocked in tests)
            try (Socket socket = createSocket(host, port);
                 BufferedWriter out = new BufferedWriter(
                         new OutputStreamWriter(socket.getOutputStream()));
                 BufferedReader in = new BufferedReader(
                         new InputStreamReader(socket.getInputStream()))
            ) {
                // Handle Lamport Clock
                lamportClock.tick();

                // Construct GET request with or without stationId
                String requestPath = "/weather.json";
                if (stationId != null && !stationId.trim().isEmpty()) {
                    // Encode stationId to handle special characters
                    String encodedStationId = java.net.URLEncoder.encode(stationId, "UTF-8");
                    requestPath += "?station_id=" + encodedStationId;
                }

                out.write("GET " + requestPath + " HTTP/1.1\r\n");
                out.write("Host: " + host + ":" + port + "\r\n");
                out.write("Connection: close\r\n");
                out.write("\r\n");
                out.flush();

                // Read response
                String statusLine = in.readLine();
                if (statusLine == null || !statusLine.contains("200")) {
                    System.out.println("Failed to get data. Status: " + statusLine);
                    return;
                }

                // Read headers
                String line;
                int contentLength = 0;
                while (!(line = in.readLine()).isEmpty()) {
                    String[] headerParts = line.split(": ", 2);
                    if (headerParts.length == 2) {
                        if (headerParts[0].equalsIgnoreCase("Content-Length")) {
                            contentLength = Integer.parseInt(headerParts[1]);
                        }
                    }
                }

                // Read body
                StringBuilder bodyBuilder = new StringBuilder();
                char[] bodyChars = new char[contentLength];
                int read = in.read(bodyChars, 0, contentLength);
                if (read > 0) {
                    bodyBuilder.append(bodyChars, 0, read);
                }
                String body = bodyBuilder.toString();

                // Parse JSON
                Map<String, Object> parsedData = jsonParser.parse(body);
                Map<String, Map<String, Object>> weatherData = (Map<String, Map<String, Object>>) (Map) parsedData;

                // If stationId is provided, filter the data
                if (stationId != null && !stationId.trim().isEmpty()) {
                    Map<String, Object> stationData = weatherData.get(stationId);
                    if (stationData != null) {
                        System.out.println("Station ID: " + stationId);
                        for (Map.Entry<String, Object> dataEntry : stationData.entrySet()) {
                            System.out.println("  " + dataEntry.getKey() + ": " + dataEntry.getValue());
                        }
                        System.out.println();
                    } else {
                        System.out.println("No data found for Station ID: " + stationId);
                    }
                } else {
                    // Display all data
                    for (Map.Entry<String, Map<String, Object>> entry : weatherData.entrySet()) {
                        System.out.println("Station ID: " + entry.getKey());
                        for (Map.Entry<String, Object> dataEntry : entry.getValue().entrySet()) {
                            System.out.println("  " + dataEntry.getKey() + ": " + dataEntry.getValue());
                        }
                        System.out.println();
                    }
                }

            }

        } catch (Exception e) {
            System.out.println("Error during GET request: " + e.getMessage());
        }
    }

    /**
     * Creates a socket connection to the specified host and port.
     * This method can be overridden in tests to mock socket connections.
     *
     * @param host The host to connect to.
     * @param port The port to connect to.
     * @return A socket connected to the specified host and port.
     * @throws IOException If an I/O error occurs when creating the socket.
     */
    public Socket createSocket(String host, int port) throws IOException {
        return new Socket(host, port);
    }

    /**
     * Main method to run the GET Client.
     *
     * @param args Command-line arguments (server URL, optional station ID).
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java -cp target/WeatherAggregationSystem-1.0-SNAPSHOT-shaded.jar com.weather.aggregation.GETClient <server_url> [station_id]");
            return;
        }

        String serverUrl = args[0];
        String stationId = args.length > 1 ? args[1] : null;

        GETClient client = new GETClient();
        client.sendGET(serverUrl, stationId);
    }
}
