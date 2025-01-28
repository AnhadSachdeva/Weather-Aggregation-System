package com.weather.aggregation;

import java.io.*;
import java.net.Socket;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles client connections to the Aggregation Server.
 */
public class ClientHandler implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);
    private Socket clientSocket;
    private LamportClock lamportClock;
    private DataStore dataStore;
    private SimpleJsonParser jsonParser;

    /**
     * Initializes the ClientHandler with the client socket and shared resources.
     *
     * @param clientSocket The client's socket connection.
     * @param lamportClock The shared Lamport clock.
     * @param dataStore    The shared data store.
     */
    public ClientHandler(Socket clientSocket, LamportClock lamportClock, DataStore dataStore) {
        this.clientSocket = clientSocket;
        this.lamportClock = lamportClock;
        this.dataStore = dataStore;
        this.jsonParser = new SimpleJsonParser();
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(
                     new InputStreamReader(clientSocket.getInputStream()));
             BufferedWriter out = new BufferedWriter(
                     new OutputStreamWriter(clientSocket.getOutputStream()))
        ) {
            // Read the request line
            String requestLine = in.readLine();
            if (requestLine == null) {
                return;
            }

            // Parse request line
            String[] requestParts = requestLine.split(" ");
            if (requestParts.length < 3) {
                sendResponse(out, "400 Bad Request", "Invalid request line");
                return;
            }

            String method = requestParts[0];
            String path = requestParts[1];
            // String httpVersion = requestParts[2];

            // Read headers
            Map<String, String> headers = new HashMap<>();
            String line;
            int contentLength = 0;
            while (!(line = in.readLine()).isEmpty()) {
                String[] headerParts = line.split(": ", 2);
                if (headerParts.length == 2) {
                    headers.put(headerParts[0], headerParts[1]);
                    if (headerParts[0].equalsIgnoreCase("Content-Length")) {
                        contentLength = Integer.parseInt(headerParts[1]);
                    }
                }
            }

            if (method.equalsIgnoreCase("PUT") && path.equalsIgnoreCase("/weather.json")) {
                handlePUT(in, out, contentLength);
            } else if (method.equalsIgnoreCase("GET") && path.startsWith("/weather.json")) {
                handleGET(out, path);
            } else {
                sendResponse(out, "400 Bad Request", "Unsupported method or path");
            }

        } catch (IOException e) {
            logger.error("IOException in ClientHandler", e);
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                logger.error("Failed to close client socket", e);
            }
        }
    }

    private void handlePUT(BufferedReader in, BufferedWriter out, int contentLength) throws IOException {
        if (contentLength == 0) {
            sendResponse(out, "204 No Content", "");
            return;
        }

        char[] bodyChars = new char[contentLength];
        int read = in.read(bodyChars, 0, contentLength);
        if (read != contentLength) {
            sendResponse(out, "400 Bad Request", "Incomplete body");
            return;
        }
        String body = new String(bodyChars);

        // Parse JSON
        Map<String, Object> jsonData;
        try {
            jsonData = jsonParser.parse(body);
        } catch (IOException e) {
            sendResponse(out, "400 Bad Request", "Invalid JSON format");
            logger.error("Failed to parse JSON body", e);
            return;
        }

        // Check for 'id'
        Object idField = jsonData.get("id");
        if (idField == null || !(idField instanceof String)) {
            sendResponse(out, "400 Bad Request", "Missing or invalid 'id' field");
            return;
        }

        // Handle Lamport Clock
        int receivedClock = 0;
        Object lamportClockValue = jsonData.get("lamportClock");
        if (lamportClockValue instanceof Number) {
            receivedClock = ((Number) lamportClockValue).intValue();
        } else if (lamportClockValue instanceof String) {
            try {
                receivedClock = Integer.parseInt((String) lamportClockValue);
            } catch (NumberFormatException e) {
                // Ignore, keep receivedClock as 0
            }
        }
        lamportClock.update(receivedClock);

        // Update local clock
        lamportClock.tick();

        // Store data
        boolean isNew = dataStore.putData(jsonData);
        if (isNew) {
            sendResponse(out, "201 Created", "Data created successfully");
        } else {
            sendResponse(out, "200 OK", "Data updated successfully");
        }
    }

    private void handleGET(BufferedWriter out, String path) throws IOException {
        // Handle query parameters
        String stationId = null;
        if (path.contains("?")) {
            String[] pathParts = path.split("\\?", 2);
            String query = pathParts[1];
            String[] queryParams = query.split("&");
            for (String param : queryParams) {
                String[] keyValue = param.split("=", 2);
                if (keyValue.length == 2 && keyValue[0].equals("station_id")) {
                    stationId = java.net.URLDecoder.decode(keyValue[1], "UTF-8");
                }
            }
        }

        // Update Lamport Clock
        lamportClock.tick();

        // Retrieve data
        Map<String, Map<String, Object>> data = dataStore.getData();

        String responseBody;
        if (stationId != null) {
            Map<String, Object> stationData = data.get(stationId);
            if (stationData != null) {
                Map<String, Object> responseData = new HashMap<>();
                responseData.put(stationId, stationData);
                responseBody = jsonParser.toJson(responseData);
            } else {
                sendResponse(out, "404 Not Found", "Station ID not found");
                return;
            }
        } else {
            responseBody = jsonParser.toJson((Map<String, Object>) (Map) data);
        }

        // Send response
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Content-Length", String.valueOf(responseBody.getBytes().length));

        sendResponse(out, "200 OK", headers, responseBody);
    }

    private void sendResponse(BufferedWriter out, String status, String body) throws IOException {
        sendResponse(out, status, new HashMap<>(), body);
    }

    private void sendResponse(BufferedWriter out, String status, Map<String, String> headers, String body) throws IOException {
        out.write("HTTP/1.1 " + status + "\r\n");
        for (Map.Entry<String, String> header : headers.entrySet()) {
            out.write(header.getKey() + ": " + header.getValue() + "\r\n");
        }
        out.write("\r\n");
        if (body != null && !body.isEmpty()) {
            out.write(body);
        }
        out.flush();
    }
}
