package com.weather.aggregation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.*;
import java.net.Socket;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class ContentServerTest {

    @TempDir
    File tempDir;

    @Test
    public void testSendPUTWithInvalidData() throws IOException {
        // Mock Socket and Streams
        Socket mockSocket = mock(Socket.class);
        InputStream mockInputStream = new ByteArrayInputStream("HTTP/1.1 400 Bad Request\r\n\r\n".getBytes());
        OutputStream mockOutputStream = new ByteArrayOutputStream();

        when(mockSocket.getInputStream()).thenReturn(mockInputStream);
        when(mockSocket.getOutputStream()).thenReturn(mockOutputStream);

        // Mock URL parsing
        ContentServer contentServer = Mockito.spy(new ContentServer());
        doReturn(mockSocket).when(contentServer).createSocket(any(String.class), anyInt());

        // Prepare invalid test data file (missing 'id' field)
        File testDataFile = new File(tempDir, "invalid_weather_data.txt");

        try (PrintWriter writer = new PrintWriter(new FileWriter(testDataFile))) {
            writer.println("temp:25.0");
            writer.println("lamportClock:1");
        } catch (IOException e) {
            fail("Failed to write test data file: " + e.getMessage());
        }

        // Capture System.out
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));

        // Execute PUT
        contentServer.sendPUT("http://localhost:4567", testDataFile.getAbsolutePath());

        // Restore System.out
        System.setOut(originalOut);

        String output = outputStream.toString();
        assertTrue(output.contains("Invalid data. 'id' field is missing."), "Output should indicate missing 'id' field");
    }
}
