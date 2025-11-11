package server;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import server.questions.Question;
import server.questions.QuestionManager;
import server.questions.AnswerManager;

public class WebSocketServer implements Runnable { 
    private static final int WS_PORT = 8081;
    private ServerSocket serverSocket;
    private static List<WebSocketClient> webSocketClients = Collections.synchronizedList(new ArrayList<>());
    private boolean running = true;

    public WebSocketServer() throws IOException {
        this.serverSocket = new ServerSocket(WS_PORT);
        System.out.println("✅ WebSocket Server started on port " + WS_PORT);
    }

    @Override
    public void run() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("🔌 New WebSocket connection from: " + clientSocket.getInetAddress());
                
                handleWebSocketHandshake(clientSocket);
                
            } catch (IOException e) {
                if (running) {
                    System.err.println("Error accepting WebSocket connection: " + e.getMessage());
                }
            }
        }
    }

    private void handleWebSocketHandshake(Socket socket) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream());

            String line;
            String key = null;
            
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                if (line.startsWith("Sec-WebSocket-Key:")) {
                    key = line.substring("Sec-WebSocket-Key:".length()).trim();
                }
            }

            if (key == null) {
                System.err.println("❌ No WebSocket key found");
                socket.close();
                return;
            }

            String acceptKey = generateAcceptKey(key);

            writer.write("HTTP/1.1 101 Switching Protocols\r\n");
            writer.write("Upgrade: websocket\r\n");
            writer.write("Connection: Upgrade\r\n");
            writer.write("Sec-WebSocket-Accept: " + acceptKey + "\r\n");
            writer.write("\r\n");
            writer.flush();

            System.out.println("✅ WebSocket handshake completed");

            WebSocketClient client = new WebSocketClient(socket);
            webSocketClients.add(client);
            QuizServer.addWebSocketClient(client);
            
            System.out.println("📊 Total WebSocket clients connected: " + webSocketClients.size());
            
            new Thread(client).start();
            
        } catch (IOException e) {
            System.err.println("Error during WebSocket handshake: " + e.getMessage());
            try {
                socket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private String generateAcceptKey(String key) {
        try {
            String magic = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hash = md.digest((key + magic).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void broadcastToWebSockets(String message) {
        System.out.println("📢 BROADCAST to " + webSocketClients.size() + " clients: " + message);
        synchronized (webSocketClients) {
            for (WebSocketClient client : webSocketClients) {
                System.out.println("  → Sending to: " + client.getStudentName());
                client.sendMessage(message);
            }
        }
    }

    public static void broadcastQuestionsToWebSockets() {
        List<Question> questions = QuestionManager.getAllQuestions();
        System.out.println("📢 Broadcasting " + questions.size() + " questions to " + webSocketClients.size() + " WebSocket clients");
        
        List<WebSocketClient> clientsToRemove = new ArrayList<>();
        
        synchronized (webSocketClients) {
            for (WebSocketClient client : webSocketClients) {
                if (!client.isRunning()) {
                    System.out.println("⚠️ Skipping inactive client: " + client.getStudentName());
                    clientsToRemove.add(client);
                    continue;
                }
                
                client.sendMessage("START_QUIZ");
                
                for (Question q : questions) {
                    String message = "QUESTION|" + q.getId() + "|" + q.getQuestion() 
                                   + "|" + String.join(",", q.getOptions());
                    client.sendMessage(message);
                }
            }
            
            for (WebSocketClient client : clientsToRemove) {
                webSocketClients.remove(client);
                System.out.println("🗑️ Removed inactive client: " + client.getStudentName());
            }
        }
        System.out.println("✅ Questions broadcast complete. Active clients: " + webSocketClients.size());
    }

    public static List<WebSocketClient> getWebSocketClients() {
        return new ArrayList<>(webSocketClients);
    }

    public static void removeWebSocketClient(WebSocketClient client) {
        webSocketClients.remove(client);
        System.out.println("🔌 WebSocket client removed: " + client.getStudentName());
        System.out.println("📊 Remaining WebSocket clients: " + webSocketClients.size());
    }
}
