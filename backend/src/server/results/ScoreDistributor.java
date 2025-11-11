package server.results;

import server.ClientHandler;
import server.QuizServer;
import server.WebSocketClient;
import server.WebSocketServer;
import server.evaluation.AnswerEvaluator;
import com.google.gson.Gson;
import java.util.*;
import java.util.stream.Collectors;


public class ScoreDistributor {
    
    private static final Gson gson = new Gson();
    

    public static void sendScoreToClient(ClientHandler clientHandler) {
        if (clientHandler == null) {
            System.err.println("❌ ClientHandler is null");
            return;
        }
        
        String studentName = clientHandler.getStudentName();
        int score = AnswerEvaluator.getTotalMarks(studentName);
        int totalQuestions = AnswerEvaluator.getTotalQuestions();
        double percentage = totalQuestions > 0 ? (score * 100.0 / totalQuestions) : 0;
        
        // Create score message
        Map<String, Object> scoreData = new HashMap<>();
        scoreData.put("studentName", studentName);
        scoreData.put("score", score);
        scoreData.put("totalQuestions", totalQuestions);
        scoreData.put("percentage", percentage);
        
        String scoreMessage = "SCORE|" + gson.toJson(scoreData);
        
        clientHandler.send(scoreMessage);
        System.out.println("📤 Score sent to TCP client " + studentName + ": " + score + "/" + totalQuestions);
    }
    

    public static void sendScoreToWebSocketClient(WebSocketClient wsClient) {
        if (wsClient == null) {
            System.err.println("❌ WebSocketClient is null");
            return;
        }
        
        String studentName = wsClient.getStudentName();
        int score = AnswerEvaluator.getTotalMarks(studentName);
        int totalQuestions = AnswerEvaluator.getTotalQuestions();
        double percentage = totalQuestions > 0 ? (score * 100.0 / totalQuestions) : 0;
        
        // Create score message
        Map<String, Object> scoreData = new HashMap<>();
        scoreData.put("studentName", studentName);
        scoreData.put("score", score);
        scoreData.put("totalQuestions", totalQuestions);
        scoreData.put("percentage", percentage);
        
        String scoreMessage = "SCORE|" + gson.toJson(scoreData);
        
        wsClient.sendMessage(scoreMessage);
        System.out.println("📤 Score sent to WebSocket client " + studentName + ": " + score + "/" + totalQuestions);
    }
    

    public static void distributeScoredToAllClients() {
        System.out.println("\n📊 DISTRIBUTING INDIVIDUAL SCORES TO ALL CLIENTS...");
        
        // Send to TCP clients
        List<ClientHandler> tcpClients = QuizServer.getConnectedClients();
        for (ClientHandler client : tcpClients) {
            sendScoreToClient(client);
        }
        
        // Send to WebSocket clients
        List<WebSocketClient> wsClients = WebSocketServer.getWebSocketClients();
        for (WebSocketClient client : wsClients) {
            sendScoreToWebSocketClient(client);
        }
        
        System.out.println("✅ Individual scores distributed to " + (tcpClients.size() + wsClients.size()) + " clients\n");
    }
    

    public static void broadcastFinalLeaderboard() {
        System.out.println("\n🏆 BROADCASTING FINAL LEADERBOARD TO ALL CLIENTS...");
        

        Map<String, Integer> scores = AnswerEvaluator.getAllScores();
        int totalQuestions = AnswerEvaluator.getTotalQuestions();
        

        List<Map<String, Object>> leaderboardList = scores.entrySet().stream()
            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
            .map(entry -> {
                Map<String, Object> item = new HashMap<>();
                item.put("rank", null);
                item.put("studentName", entry.getKey());
                item.put("score", entry.getValue());
                item.put("totalQuestions", totalQuestions);
                item.put("percentage", totalQuestions > 0 ? (entry.getValue() * 100.0 / totalQuestions) : 0);
                return item;
            })
            .collect(Collectors.toList());
        

        for (int i = 0; i < leaderboardList.size(); i++) {
            leaderboardList.get(i).put("rank", i + 1);
        }
        

        Map<String, Object> leaderboardData = new HashMap<>();
        leaderboardData.put("leaderboard", leaderboardList);
        leaderboardData.put("timestamp", System.currentTimeMillis());
        leaderboardData.put("totalStudents", leaderboardList.size());
        
        String leaderboardMessage = "LEADERBOARD|" + gson.toJson(leaderboardData);
        

        List<ClientHandler> tcpClients = QuizServer.getConnectedClients();
        for (ClientHandler client : tcpClients) {
            client.send(leaderboardMessage);
            System.out.println("📤 Leaderboard sent to TCP client: " + client.getStudentName());
        }
        

        List<WebSocketClient> wsClients = WebSocketServer.getWebSocketClients();
        for (WebSocketClient client : wsClients) {
            client.sendMessage(leaderboardMessage);
            System.out.println("📤 Leaderboard sent to WebSocket client: " + client.getStudentName());
        }
        
        System.out.println("✅ Final leaderboard broadcast to " + (tcpClients.size() + wsClients.size()) + " clients\n");
    }
    

    public static void printLeaderboard() {
        Map<String, Integer> scores = AnswerEvaluator.getAllScores();
        int totalQuestions = AnswerEvaluator.getTotalQuestions();
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("🏆 FINAL LEADERBOARD");
        System.out.println("=".repeat(60));
        System.out.println(String.format("%-6s | %-20s | %-10s | %-10s", "Rank", "Student Name", "Score", "Percentage"));
        System.out.println("-".repeat(60));
        
        scores.entrySet().stream()
            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
            .forEach(entry -> {
                int rank = scores.entrySet().stream()
                    .filter(e -> e.getValue() > entry.getValue())
                    .collect(Collectors.toList())
                    .size() + 1;
                
                String name = entry.getKey();
                int score = entry.getValue();
                double percentage = totalQuestions > 0 ? (score * 100.0 / totalQuestions) : 0;
                System.out.println(String.format("%-6d | %-20s | %-10s | %.2f%%", 
                    rank, name, score + "/" + totalQuestions, percentage));
            });
        
        System.out.println("=".repeat(60) + "\n");
    }
    

    public static void distributeAllResults() {
        System.out.println("\n" + "🎯".repeat(30));
        System.out.println("STARTING COMPLETE RESULT DISTRIBUTION PROCESS");
        System.out.println("🎯".repeat(30) + "\n");
        

        distributeScoredToAllClients();
        

        broadcastFinalLeaderboard();
        

        printLeaderboard();
        
        System.out.println("✅ COMPLETE RESULT DISTRIBUTION FINISHED\n");
    }
}
