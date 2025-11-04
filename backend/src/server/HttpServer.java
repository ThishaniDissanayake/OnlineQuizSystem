package server;

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.util.List;

public class HttpServer {
    private com.sun.net.httpserver.HttpServer server;
    private static final int HTTP_PORT = 8080;

    public void start() throws IOException {
        server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(HTTP_PORT), 0);
        
        // Enable CORS for all routes
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

    // Handler for /api/students - returns list of connected students
    static class StudentsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Enable CORS
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
            
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("GET".equals(exchange.getRequestMethod())) {
                List<ClientHandler> clients = QuizServer.getConnectedClients();
                
                // Build JSON response
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

    // Handler for /api/quiz/start - starts the quiz
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
                // Broadcast START_QUIZ to all connected clients
                List<ClientHandler> clients = QuizServer.getConnectedClients();
                for (ClientHandler client : clients) {
                    client.send("START_QUIZ");
                }

                String response = "{\"success\":true,\"message\":\"Quiz started\",\"studentCount\":" + clients.size() + "}";
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

    // Handler for /api/status - returns server status
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
