package server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import server.questions.Question;
import server.questions.QuestionManager;

public class QuizServer {
    private static final int PORT = 5000;
    private static ServerSocket serverSocket;
    private static ExecutorService threadPool = Executors.newFixedThreadPool(10);
    private static List<ClientHandler> connectedClients = Collections.synchronizedList(new ArrayList<>());
    private static HttpServer httpServer;
    private static WebSocketServer webSocketServer;

    public static void main(String[] args) {
        try {
            // Start HTTP API Server
            httpServer = new HttpServer();
            httpServer.start();

            // Start WebSocket Server
            webSocketServer = new WebSocketServer();
            new Thread(webSocketServer).start();

            // Start TCP Server for Java clients
            serverSocket = new ServerSocket(PORT);
            System.out.println("✅ Quiz Server (TCP) started on port " + PORT);
            System.out.println("📊 Admin Dashboard: http://localhost:8080/admin/dashboard.html");
            System.out.println("👨‍🎓 Student Portal: http://localhost:8080/student/student.html");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("🎯 New TCP client connected: " + clientSocket.getInetAddress().getHostName());

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
                if (webSocketServer != null) webSocketServer.stop();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Return TCP connected clients
    public static List<ClientHandler> getConnectedClients() {
        return connectedClients;
    }
    
    // WebSocket client management
    public static void addWebSocketClient(WebSocketClient client) {
        // Delegated to WebSocketServer
        System.out.println("✅ WebSocket client added via server");
    }

    public static void removeWebSocketClient(WebSocketClient client) {
        // Delegated to WebSocketServer
        System.out.println("❌ WebSocket client removed via server");
    }

    public static List<WebSocketClient> getWebSocketClients() {
        return WebSocketServer.getWebSocketClients();
    }

    public static void removeClient(ClientHandler handler) {
        connectedClients.remove(handler);
        System.out.println("Client removed. Connected TCP clients: " + connectedClients.size());
    }

    // Broadcast to both TCP and WebSocket clients
    public static void broadcastQuestions() {
        List<Question> questions = QuestionManager.getAllQuestions();
        System.out.println("📢 Broadcasting " + questions.size() + " questions to all clients...");

        // TCP clients
        synchronized (connectedClients) {
            for (ClientHandler client : connectedClients) {
                client.send("START_QUIZ");
                for (Question q : questions) {
                    client.sendQuestion(q);
                }
            }
        }
        
        // WebSocket clients
        WebSocketServer.broadcastQuestionsToWebSockets();

        System.out.println("✅ Questions broadcast complete!");
    }

    /**
     * Broadcast individual scores to all connected clients (TCP & WebSocket)
     */
    public static void broadcastScores() {
        System.out.println("\n📊 Broadcasting individual scores to all clients...");
        
        // Import and use ScoreDistributor
        try {
            server.results.ScoreDistributor.distributeScoredToAllClients();
        } catch (Exception e) {
            System.err.println("❌ Error broadcasting scores: " + e.getMessage());
        }
    }

    /**
     * Broadcast final leaderboard to all connected clients (TCP & WebSocket)
     */
    public static void broadcastLeaderboard() {
        System.out.println("\n🏆 Broadcasting final leaderboard to all clients...");
        
        // Import and use ScoreDistributor
        try {
            server.results.ScoreDistributor.broadcastFinalLeaderboard();
        } catch (Exception e) {
            System.err.println("❌ Error broadcasting leaderboard: " + e.getMessage());
        }
    }

    /**
     * Distribute all results: scores + leaderboard (blocking)
     */
    public static void distributeAllResults() {
        System.out.println("\n" + "🎯".repeat(20));
        System.out.println("QUIZ COMPLETE - DISTRIBUTING RESULTS");
        System.out.println("🎯".repeat(20));
        
        try {
            server.results.ScoreDistributor.distributeAllResults();
        } catch (Exception e) {
            System.err.println("❌ Error distributing results: " + e.getMessage());
        }
    }

    /**
     * Distribute all results asynchronously (non-blocking)
     * Returns a CompletableFuture that completes when distribution is done
     */
    public static java.util.concurrent.CompletableFuture<Void> distributeAllResultsAsync() {
        System.out.println("\n" + "🚀".repeat(20));
        System.out.println("QUIZ COMPLETE - ASYNC RESULT DISTRIBUTION");
        System.out.println("🚀".repeat(20));
        
        try {
            return server.results.ResultBroadcaster.distributeResultsAsync();
        } catch (Exception e) {
            System.err.println("❌ Error in async distribution: " + e.getMessage());
            return java.util.concurrent.CompletableFuture.failedFuture(e);
        }
    }
}