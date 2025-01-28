package com.weather.aggregation;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.*;
import java.net.Socket;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class GETClientTest {

    @Test
    public void testSendGETSuccess() throws IOException {
        // Mock Socket and Streams
        Socket mockSocket = mock(Socket.class);
        String mockResponse = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/json\r\n" +
                "Content-Length: 95\r\n" +
                "\r\n" +
                "{\"testStation\":{\"id\":\"testStation\",\"temp\":25.0,\"lamportClock\":1}}";
        InputStream mockInputStream = new ByteArrayInputStream(mockResponse.getBytes());
        OutputStream mockOutputStream = new ByteArrayOutputStream();

        when(mockSocket.getInputStream()).thenReturn(mockInputStream);
        when(mockSocket.getOutputStream()).thenReturn(mockOutputStream);

        // Mock URL parsing
        GETClient client = Mockito.spy(new GETClient());
        doReturn(mockSocket).when(client).createSocket(any(String.class), anyInt());

        // Capture System.out
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));

        // Execute GET
        client.sendGET("http://localhost:4567", "testStation");

        // Restore System.out
        System.setOut(originalOut);

        String output = outputStream.toString();
        assertTrue(output.contains("Station ID: testStation"), "Output should contain station ID");
        assertTrue(output.contains("temp: 25.0"), "Output should contain temperature");
    }
}
