package com.weather.aggregation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;

/**
 * Utility class for handling HTTP-related operations.
 */
public class HttpUtils {

    /**
     * Sends an HTTP response with the specified status, headers, and body.
     *
     * @param out     BufferedWriter to write the response to.
     * @param status  HTTP status line (e.g., "200 OK").
     * @param headers Map of HTTP headers.
     * @param body    Response body as a string.
     * @throws IOException If an I/O error occurs.
     */
    public static void sendResponse(BufferedWriter out, String status, Map<String, String> headers, String body) throws IOException {
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
