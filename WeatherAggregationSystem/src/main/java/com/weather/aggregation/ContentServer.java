package com.weather.aggregation;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.*;

/**
 * The Content Server reads weather data from a file and sends it to the Aggregation Server.
 */
public class ContentServer {
    private static final SimpleJsonParser jsonParser = new SimpleJsonParser();
    private LamportClock lamportClock;

    /**
     * Initializes the Content Server.
     */
    public ContentServer() {
        lamportClock = new LamportClock();
    }

    /**
     * Sends a PUT request with weather data to the Aggregation Server.
     *
     * @param serverUrl The URL of the Aggregation Server.
     * @param filePath  The path to the data file.
     */
    public void sendPUT(String serverUrl, String filePath) {
        try {
            // Parse server URL
            String[] urlParts = parseServerUrl(serverUrl);
            String host = urlParts[0];
            int port = urlParts[1] != null ? Integer.parseInt(urlParts[1]) : 80;

            // Read and assemble JSON data from file
            Map<String, Object> weatherData = readDataFromFile(filePath);
            if (weatherData == null || !weatherData.containsKey("id")) {
                System.out.println("Invalid data. 'id' field is missing.");
                return;
            }

            // Add Lamport Clock to data
            lamportClock.tick();
            weatherData.put("lamportClock", lamportClock.getTime());

            String jsonData = jsonParser.toJson(weatherData);
            byte[] jsonBytes = jsonData.getBytes("UTF-8");

            // Use the createSocket method (can be mocked in tests)
            try (Socket socket = createSocket(host, port);
                 BufferedWriter out = new BufferedWriter(
                         new OutputStreamWriter(socket.getOutputStream()));
                 BufferedReader in = new BufferedReader(
                         new InputStreamReader(socket.getInputStream()))
            ) {
                // Construct PUT request
                out.write("PUT /weather.json HTTP/1.1\r\n");
                out.write("Host: " + host + ":" + port + "\r\n");
                out.write("Content-Type: application/json; utf-8\r\n");
                out.write("Content-Length: " + jsonBytes.length + "\r\n");
                out.write("Connection: close\r\n");
                out.write("\r\n");
                out.write(jsonData);
                out.flush();

                // Read response
                String statusLine = in.readLine();
                if (statusLine == null) {
                    System.out.println("No response from server.");
                    return;
                }

                if (statusLine.contains("201") || statusLine.contains("200")) {
                    System.out.println("Data successfully uploaded. Status: " + statusLine);
                } else if (statusLine.contains("204")) {
                    System.out.println("No Content received. Status: " + statusLine);
                } else {
                    System.out.println("Failed to upload data. Status: " + statusLine);
                }

            }

        } catch (Exception e) {
            System.out.println("Error during PUT request: " + e.getMessage());
        }
    }

    /**
     * Parses the server URL into host and port.
     *
     * @param serverUrl The server URL (e.g., http://localhost:4567).
     * @return An array where index 0 is the host and index 1 is the port (as a String).
     */
    private String[] parseServerUrl(String serverUrl) {
        String host = "";
        String port = null;
        try {
            URL url = new URL(serverUrl);
            host = url.getHost();
            if (url.getPort() != -1) {
                port = String.valueOf(url.getPort());
            } else {
                port = "80"; // Default port for HTTP
            }
        } catch (Exception e) {
            System.out.println("Invalid server URL format. Using defaults.");
            host = "localhost";
            port = "80";
        }
        return new String[]{host, port};
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
     * Reads and parses the data file into a Map.
     *
     * @param filePath The path to the data file.
     * @return A Map containing the weather data, or null if parsing fails.
     */
    private Map<String, Object> readDataFromFile(String filePath) {
        Map<String, Object> data = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            int lineNumber = 0; // To track line numbers for debugging
            while ((line = br.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue; // Skip empty lines and comments

                // Remove trailing commas if present
                if (line.endsWith(",")) {
                    line = line.substring(0, line.length() - 1).trim();
                }

                String[] parts = line.split(":", 2);
                if (parts.length != 2) {
                    System.err.println("Invalid line format at line " + lineNumber + ": " + line);
                    continue;
                }
                String key = parts[0].trim();
                String value = parts[1].trim();

                // Remove surrounding quotes if present
                key = removeSurroundingQuotes(key);
                value = removeSurroundingQuotes(value);

                // Convert numerical values appropriately
                try {
                    if (isDoubleKey(key)) {
                        data.put(key, Double.parseDouble(value));
                    } else if (isIntegerKey(key)) {
                        data.put(key, Integer.parseInt(value));
                    } else {
                        data.put(key, value);
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Invalid number format for key '" + key + "': " + value + " at line " + lineNumber);
                    data.put(key, value); // Keep as string if parsing fails
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("Data file not found: " + filePath);
            return null;
        } catch (IOException e) {
            System.err.println("Error reading data file: " + e.getMessage());
            return null;
        }
        return data;
    }

    /**
     * Removes surrounding single or double quotes from a string, if present.
     *
     * @param str The input string.
     * @return The string without surrounding quotes.
     */
    private String removeSurroundingQuotes(String str) {
        if ((str.startsWith("\"") && str.endsWith("\"")) ||
            (str.startsWith("'") && str.endsWith("'"))) {
            return str.substring(1, str.length() - 1).trim();
        }
        return str;
    }

    /**
     * Determines if a key should be parsed as a double.
     *
     * @param key The key to check.
     * @return True if the key corresponds to a double value, false otherwise.
     */
    private boolean isDoubleKey(String key) {
        Set<String> doubleKeys = new HashSet<>(Arrays.asList(
                "lat", "lon", "air_temp", "apparent_t", "dewpt", "press", "wind_spd_kmh", "wind_spd_kt", "temp"
        ));
        return doubleKeys.contains(key);
    }

    /**
     * Determines if a key should be parsed as an integer.
     *
     * @param key The key to check.
     * @return True if the key corresponds to an integer value, false otherwise.
     */
    private boolean isIntegerKey(String key) {
        Set<String> integerKeys = new HashSet<>(Collections.singletonList("rel_hum"));
        return integerKeys.contains(key);
    }

    /**
     * Main method to run the Content Server.
     *
     * @param args Command-line arguments (server URL, data file path).
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java -cp target/WeatherAggregationSystem-1.0-SNAPSHOT-shaded.jar com.weather.aggregation.ContentServer <server_url> <data_file_path>");
            return;
        }

        String serverUrl = args[0];
        String filePath = args[1];

        ContentServer contentServer = new ContentServer();
        contentServer.sendPUT(serverUrl, filePath);
    }
}
