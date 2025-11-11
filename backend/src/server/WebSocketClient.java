package server;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import server.questions.AnswerManager;


public class WebSocketClient implements Runnable {
    private Socket socket;
    private InputStream input;
    private OutputStream output;
    private String studentName;
    private volatile boolean running = true;
    private volatile boolean hasSubmitted = false;
    private final Object writeLock = new Object();
    private final String clientId;

    public WebSocketClient(Socket socket) {
        this.socket = socket;

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
            

            List<WebSocketClient> allClients = WebSocketServer.getWebSocketClients();
            for (WebSocketClient client : allClients) {
                if (client == this) {
                    System.out.println("   ✅ THIS CLIENT: " + client.getStudentName() + " [" + client.getClientId() + "] - WILL RECEIVE ACK");
                } else {
                    System.out.println("   ❌ OTHER CLIENT: " + client.getStudentName() + " [" + client.getClientId() + "] - WILL NOT RECEIVE ACK");
                }
            }
            

            sendPrivateMessage("SUBMISSION_ACK");
            
            System.out.println("✅ [" + clientId + "] SUBMISSION_ACK sent successfully to " + studentName);
            System.out.println("📊 Other students still connected: " + (WebSocketServer.getWebSocketClients().size() - 1));
        }
    }


    private void sendPrivateMessage(String message) {
        System.out.println("🔒 [" + clientId + "] PRIVATE MESSAGE to " + getStudentName() + ": " + message);
        

        if (!running || socket == null || socket.isClosed()) {
            System.err.println("❌ [" + clientId + "] Cannot send - connection closed");
            return;
        }
        
        synchronized (writeLock) {
            if (!running || socket == null || socket.isClosed()) {
                System.err.println("❌ [" + clientId + "] Cannot send - connection closed (after lock)");
                return;
            }
            
            try {
                byte[] payload = message.getBytes(StandardCharsets.UTF_8);
                byte[] frame = createFrame(payload);
                

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

        if (!running || socket == null || socket.isClosed()) {
            System.out.println("⚠️ [" + clientId + "] Cannot send to " + getStudentName() + " - connection closed");
            return;
        }
        

        synchronized (writeLock) {
            if (!running || socket == null || socket.isClosed()) {
                System.out.println("⚠️ [" + clientId + "] Cannot send to " + getStudentName() + " - connection closed (after lock)");
                return;
            }
            
            try {
                byte[] payload = message.getBytes(StandardCharsets.UTF_8);
                byte[] frame = createFrame(payload);
                

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
