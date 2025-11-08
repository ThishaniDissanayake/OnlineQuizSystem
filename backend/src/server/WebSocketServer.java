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

class WebSocketClient implements Runnable {
    private Socket socket;
    private InputStream input;
    private OutputStream output;
    private String studentName;
    private volatile boolean running = true;
    private volatile boolean hasSubmitted = false;
    private final Object writeLock = new Object();
    private final String clientId; // Unique identifier for this client

    public WebSocketClient(Socket socket) {
        this.socket = socket;
        // Generate unique client ID
        this.clientId = "WS-" + System.currentTimeMillis() + "-" + socket.getPort();
        try {
            this.input = socket.getInputStream();
            this.output = socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            System.out.println("👂 WebSocket client thread started for " + clientId);
            
            while (running) {
                byte[] frame = readFrame();
                if (frame == null) {
                    System.out.println("⚠️ Received null frame from " + getStudentName() + " - connection closing");
                    break;
                }
                
                String message = new String(frame, StandardCharsets.UTF_8);
                handleMessage(message);
            }
        } catch (IOException e) {
            System.out.println("⚠️ WebSocket connection closed for " + getStudentName() + ": " + e.getMessage());
        } finally {
            System.out.println("🧹 Finally block: Calling cleanup for " + getStudentName());
            cleanup();
        }
    }

    private void handleMessage(String message) {
        System.out.println("📨 [" + clientId + "] Received from " + getStudentName() + ": " + message);
        
        if (studentName == null) {
            studentName = message.trim();
            System.out.println("👤 [" + clientId + "] NEW STUDENT REGISTERED: " + studentName);
            sendMessage("Welcome, " + studentName + "! Please wait for the quiz to start...");
        } 
        else if (message.startsWith("ANSWER|")) {
            String[] parts = message.split("\\|");
            if (parts.length == 3) {
                int questionId = Integer.parseInt(parts[1]);
                String answer = parts[2];
                AnswerManager.recordAnswer(studentName, questionId, answer);
                System.out.println("📝 [" + clientId + "] Answer saved: " + studentName + " Q" + questionId + " → " + answer);
            }
        } 
        else if (message.equals("SUBMIT_COMPLETE")) {
            if (hasSubmitted) {
                System.out.println("⚠️ [" + clientId + "] " + studentName + " already submitted, ignoring duplicate");
                return;
            }
            
            hasSubmitted = true;
            System.out.println("✅ [" + clientId + "] " + studentName + " SUBMITTED QUIZ");
            System.out.println("📤 [" + clientId + "] Sending SUBMISSION_ACK to " + studentName + " ONLY");
            System.out.println("🚫 [" + clientId + "] NOT sending to other clients");
            
            // List all connected clients for verification
            List<WebSocketClient> allClients = WebSocketServer.getWebSocketClients();
            for (WebSocketClient client : allClients) {
                if (client == this) {
                    System.out.println("   ✅ THIS CLIENT: " + client.getStudentName() + " [" + client.getClientId() + "] - WILL RECEIVE ACK");
                } else {
                    System.out.println("   ❌ OTHER CLIENT: " + client.getStudentName() + " [" + client.getClientId() + "] - WILL NOT RECEIVE ACK");
                }
            }
            
            // CRITICAL FIX: Send acknowledgment ONLY to THIS specific client
            sendPrivateMessage("SUBMISSION_ACK");
            
            System.out.println("✅ [" + clientId + "] SUBMISSION_ACK sent successfully to " + studentName);
            System.out.println("📊 Other students still connected: " + (WebSocketServer.getWebSocketClients().size() - 1));
        }
    }

    /**
     * CRITICAL: Private message method that ONLY sends to THIS client
     * Includes additional safeguards to prevent cross-client communication
     */
    private void sendPrivateMessage(String message) {
        System.out.println("🔒 [" + clientId + "] PRIVATE MESSAGE to " + getStudentName() + ": " + message);
        
        // Verify this is the correct client
        if (!running || socket == null || socket.isClosed()) {
            System.err.println("❌ [" + clientId + "] Cannot send - connection closed");
            return;
        }
        
        synchronized (writeLock) {
            // Double-check after acquiring lock
            if (!running || socket == null || socket.isClosed()) {
                System.err.println("❌ [" + clientId + "] Cannot send - connection closed (after lock)");
                return;
            }
            
            try {
                byte[] payload = message.getBytes(StandardCharsets.UTF_8);
                byte[] frame = createFrame(payload);
                
                // Write atomically
                output.write(frame);
                output.flush();
                
                System.out.println("✅ [" + clientId + "] Private message sent successfully to " + getStudentName());
                
            } catch (IOException e) {
                System.err.println("❌ [" + clientId + "] Failed to send private message: " + e.getMessage());
                running = false;
            }
        }
    }

    public void sendMessage(String message) {
        // Check connection state BEFORE acquiring lock
        if (!running || socket == null || socket.isClosed()) {
            System.out.println("⚠️ [" + clientId + "] Cannot send to " + getStudentName() + " - connection closed");
            return;
        }
        
        // Synchronize on THIS client's lock only
        synchronized (writeLock) {
            // Double-check after acquiring lock
            if (!running || socket == null || socket.isClosed()) {
                System.out.println("⚠️ [" + clientId + "] Cannot send to " + getStudentName() + " - connection closed (after lock)");
                return;
            }
            
            try {
                byte[] payload = message.getBytes(StandardCharsets.UTF_8);
                byte[] frame = createFrame(payload);
                
                // Write and flush atomically
                output.write(frame);
                output.flush();
                
                if (!message.startsWith("QUESTION|")) {
                    System.out.println("📤 [" + clientId + "] Sent to " + getStudentName() + ": " + message);
                }
            } catch (IOException e) {
                System.err.println("❌ [" + clientId + "] Failed to send to " + getStudentName() + ": " + e.getMessage());
                System.out.println("🔌 Marking connection as closed for " + getStudentName());
                running = false;
            }
        }
    }

    private byte[] readFrame() throws IOException {
        int b = input.read();
        if (b == -1) return null;

        boolean fin = (b & 0x80) != 0;
        int opcode = b & 0x0F;

        if (opcode == 0x08) {
            System.out.println("🔒 [" + clientId + "] Close frame received from " + getStudentName());
            return null;
        }

        b = input.read();
        if (b == -1) return null;

        boolean masked = (b & 0x80) != 0;
        int payloadLength = b & 0x7F;

        if (payloadLength == 126) {
            int b1 = input.read();
            int b2 = input.read();
            if (b1 == -1 || b2 == -1) return null;
            payloadLength = (b1 << 8) | b2;
        } else if (payloadLength == 127) {
            throw new IOException("Payload too large");
        }

        byte[] mask = null;
        if (masked) {
            mask = new byte[4];
            int read = input.read(mask);
            if (read != 4) return null;
        }

        byte[] payload = new byte[payloadLength];
        int totalRead = 0;
        while (totalRead < payloadLength) {
            int read = input.read(payload, totalRead, payloadLength - totalRead);
            if (read == -1) return null;
            totalRead += read;
        }

        if (masked && mask != null) {
            for (int i = 0; i < payload.length; i++) {
                payload[i] ^= mask[i % 4];
            }
        }

        return payload;
    }

    private byte[] createFrame(byte[] payload) {
        int length = payload.length;
        byte[] frame;

        if (length <= 125) {
            frame = new byte[2 + length];
            frame[0] = (byte) 0x81;
            frame[1] = (byte) length;
            System.arraycopy(payload, 0, frame, 2, length);
        } else if (length <= 65535) {
            frame = new byte[4 + length];
            frame[0] = (byte) 0x81;
            frame[1] = 126;
            frame[2] = (byte) (length >> 8);
            frame[3] = (byte) length;
            System.arraycopy(payload, 0, frame, 4, length);
        } else {
            throw new RuntimeException("Payload too large");
        }

        return frame;
    }

    public String getStudentName() {
        return studentName != null ? studentName : clientId;
    }

    public String getClientName() {
        return getStudentName();
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public boolean isRunning() {
        return running;
    }

    private void cleanup() {
        running = false;
        System.out.println("🧹 [" + clientId + "] Cleaning up connection for: " + getStudentName());
        
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        WebSocketServer.removeWebSocketClient(this);
        QuizServer.removeWebSocketClient(this);
        
        System.out.println("🔌 [" + clientId + "] " + getStudentName() + " disconnected and cleaned up");
    }
}