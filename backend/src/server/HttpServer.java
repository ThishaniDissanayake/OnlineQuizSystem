package server;

import com.sun.net.httpserver.*;
import server.questions.QuestionManager;
import server.questions.Question;
import com.google.gson.Gson;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.List;

public class HttpServer {
    private com.sun.net.httpserver.HttpServer server;
    private static final int HTTP_PORT = 8080;

    public void start() throws IOException {
        // Load questions from backend/resources/questions.json before server starts
        QuestionManager.loadQuestions();

        server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(HTTP_PORT), 0);
        
        // --- Question API ---
        server.createContext("/api/questions", new QuestionsHandler());
        
        // --- Other APIs ---
        server.createContext("/api/students", new StudentsHandler());
        server.createContext("/api/quiz/start", new StartQuizHandler());
        server.createContext("/api/status", new StatusHandler());
        
        server.setExecutor(null);
        server.start();
        System.out.println("✅ HTTP API Server started on port " + HTTP_PORT);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    // ---  Handler for /api/questions ---
    static class QuestionsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("GET".equals(exchange.getRequestMethod())) {
                List<Question> questions = QuestionManager.getAllQuestions();
                String jsonResponse = new Gson().toJson(questions);

                byte[] response = jsonResponse.getBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length);
                OutputStream os = exchange.getResponseBody();
                os.write(response);
                os.close();
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    // --- Handler for /api/students ---
    static class StudentsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
            
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("GET".equals(exchange.getRequestMethod())) {
                List<ClientHandler> clients = QuizServer.getConnectedClients();
                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < clients.size(); i++) {
                    ClientHandler client = clients.get(i);
                    json.append("{")
                        .append("\"id\":").append(i + 1).append(",")
                        .append("\"name\":\"").append(client.getClientName()).append("\",")
                        .append("\"connectedAt\":\"").append(new java.util.Date()).append("\"")
                        .append("}");
                    if (i < clients.size() - 1) json.append(",");
                }
                json.append("]");

                byte[] response = json.toString().getBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length);
                OutputStream os = exchange.getResponseBody();
                os.write(response);
                os.close();
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    // --- Handler for /api/quiz/start ---
    static class StartQuizHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("POST".equals(exchange.getRequestMethod())) {
                List<ClientHandler> clients = QuizServer.getConnectedClients();
                
                // Member 2: Broadcast questions using ObjectOutputStream
                QuizServer.broadcastQuestions();
                
                // Also send START_QUIZ message via text
                for (ClientHandler client : clients) {
                    client.send("START_QUIZ");
                }

                String response = "{\"success\":true,\"message\":\"Quiz started and questions broadcast via Socket\",\"studentCount\":" + clients.size() + "}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    // --- Handler for /api/status ---
    static class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");

            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("GET".equals(exchange.getRequestMethod())) {
                String response = "{\"status\":\"online\",\"port\":5000,\"connectedStudents\":" 
                    + QuizServer.getConnectedClients().size() + "}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }
}
