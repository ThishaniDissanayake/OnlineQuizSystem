package server;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class QuizServer {
    private static final int PORT = 5000;
    private static ServerSocket serverSocket;
    private static ExecutorService threadPool = Executors.newFixedThreadPool(10);
    private static List<ClientHandler> connectedClients = Collections.synchronizedList(new ArrayList<>());
    private static HttpServer httpServer;

    public static void main(String[] args) {
        try {
            // Start HTTP API server
            httpServer = new HttpServer();
            httpServer.start();
            
            serverSocket = new ServerSocket(PORT);
            System.out.println("✅ Quiz Server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("🎯 New client connected: " + clientSocket.getInetAddress().getHostName());
                
                ClientHandler handler = new ClientHandler(clientSocket);
                connectedClients.add(handler);
                threadPool.execute(handler);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (serverSocket != null) serverSocket.close();
                if (httpServer != null) httpServer.stop();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Method for future use (to get connected student names)
    public static List<ClientHandler> getConnectedClients() {
        return connectedClients;
    }
    
    public static void removeClient(ClientHandler handler) {
        connectedClients.remove(handler);
        System.out.println("Client removed. Connected clients: " + connectedClients.size());
    }
}
