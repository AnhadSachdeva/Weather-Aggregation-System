package com.weather.aggregation;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.Socket;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ClientHandlerTest {

    @Test
    public void testHandlePUTValidData() throws IOException {
        // Mock Socket and Streams
        Socket mockSocket = mock(Socket.class);
        String requestBody = "{\"id\":\"testStation\",\"temp\":25.0,\"lamportClock\":1}";
        String request = "PUT /weather.json HTTP/1.1\r\n" +
                "Host: localhost:4567\r\n" +
                "Content-Type: application/json\r\n" +
                "Content-Length: " + requestBody.getBytes().length + "\r\n" +
                "\r\n" +
                requestBody;
        InputStream mockInputStream = new ByteArrayInputStream(request.getBytes());
        ByteArrayOutputStream mockOutputStream = new ByteArrayOutputStream();

        when(mockSocket.getInputStream()).thenReturn(mockInputStream);
        when(mockSocket.getOutputStream()).thenReturn(mockOutputStream);

        // Initialize ClientHandler
        LamportClock clock = new LamportClock();
        DataStore store = DataStore.getInstance();
        store.clearData();

        ClientHandler handler = new ClientHandler(mockSocket, clock, store);
        handler.run();

        // Verify response
        String response = mockOutputStream.toString();
        assertTrue(response.contains("201 Created") || response.contains("200 OK"), "Response should indicate creation or update");

        // Verify data is stored
        Map<String, Object> storedData = store.getData().get("testStation");
        assertNotNull(storedData, "Data should be stored");
        assertEquals(25.0, storedData.get("temp"), "Temperature should be 25.0");
    }

    @Test
    public void testHandleGETInvalidStationId() throws IOException {
        // Mock Socket and Streams
        Socket mockSocket = mock(Socket.class);
        String request = "GET /weather.json?station_id=invalidStation HTTP/1.1\r\n" +
                "Host: localhost:4567\r\n" +
                "\r\n";
        InputStream mockInputStream = new ByteArrayInputStream(request.getBytes());
        ByteArrayOutputStream mockOutputStream = new ByteArrayOutputStream();

        when(mockSocket.getInputStream()).thenReturn(mockInputStream);
        when(mockSocket.getOutputStream()).thenReturn(mockOutputStream);

        // Initialize ClientHandler
        LamportClock clock = new LamportClock();
        DataStore store = DataStore.getInstance();
        store.clearData();

        ClientHandler handler = new ClientHandler(mockSocket, clock, store);
        handler.run();

        // Verify response
        String response = mockOutputStream.toString();
        assertTrue(response.contains("404 Not Found"), "Response should indicate station ID not found");
    }
}
