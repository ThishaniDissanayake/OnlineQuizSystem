package server;

import com.sun.net.httpserver.*;
import server.questions.QuestionManager;
import server.questions.Question;
import server.evaluation.AnswerEvaluator;
import server.results.ResultDistributor;
import com.google.gson.Gson;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

public class HttpServer {
    private com.sun.net.httpserver.HttpServer server;
    private static final int HTTP_PORT = 8080;

    public void start() throws IOException {
        // Load questions from backend/resources/questions.json before server starts
        QuestionManager.loadQuestions();

        server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(HTTP_PORT), 0);
        
        // --- API Routes (higher priority) ---
        server.createContext("/api/questions", new QuestionsHandler());
        server.createContext("/api/questions/add", new AddQuestionHandler());
        server.createContext("/api/questions/update", new UpdateQuestionHandler());
        server.createContext("/api/questions/delete", new DeleteQuestionHandler());
        server.createContext("/api/students", new StudentsHandler());
        server.createContext("/api/quiz/start", new StartQuizHandler());
        server.createContext("/api/status", new StatusHandler());
        
        // --- Member 4: Answer Evaluation & Scoring API Routes ---
        server.createContext("/api/results", new ResultsHandler());
        server.createContext("/api/results/student", new StudentResultHandler());
        server.createContext("/api/evaluate", new EvaluateAnswersHandler());
        
        // --- Static File Serving (catch-all, lowest priority) ---
        server.createContext("/", new StaticFileHandler());
        
        server.setExecutor(null);
        server.start();
        System.out.println("✅ HTTP API Server started on port " + HTTP_PORT);
        System.out.println("📄 Serving static files from: ./frontend/");
        System.out.println("🎓 Student Portal: http://localhost:8080/pages/student.html");
        System.out.println("📊 Admin Portal: http://localhost:8080/pages/admin.html");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    // --- Static File Handler ---
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestPath = exchange.getRequestURI().getPath();
            
            // Skip API routes
            if (requestPath.startsWith("/api/")) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            
            // Default to index.html
            if (requestPath.equals("/")) {
                requestPath = "/index.html";
            }
            
            System.out.println("📄 Static request: " + requestPath);
            System.out.println("   Current directory: " + new File(".").getAbsolutePath());
            
            // Try multiple possible locations
            File file = null;
            String[] possiblePaths = {
                ".." + File.separator + "frontend" + requestPath,  // Go up one level to find frontend
                "frontend" + requestPath,
                "." + requestPath
            };
            
            for (String path : possiblePaths) {
                File testFile = new File(path);
                System.out.println("   Trying: " + testFile.getAbsolutePath() + " - Exists: " + testFile.exists());
                if (testFile.exists() && !testFile.isDirectory()) {
                    file = testFile;
                    break;
                }
            }
            
            if (file == null) {
                System.out.println("   ❌ Not found in any location");
                String response = "404 - File Not Found: " + requestPath + 
                                "\nCurrent dir: " + new File(".").getAbsolutePath() +
                                "\nTried: frontend" + requestPath;
                exchange.sendResponseHeaders(404, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
                return;
            }
            
            // Determine content type
            String contentType = getContentType(requestPath);
            
            System.out.println("   ✅ Serving: " + file.getAbsolutePath() + " (" + contentType + ")");
            
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
            exchange.sendResponseHeaders(200, fileBytes.length);
            
            OutputStream os = exchange.getResponseBody();
            os.write(fileBytes);
            os.close();
        }
        
        private String getContentType(String path) {
            if (path.endsWith(".html")) return "text/html; charset=UTF-8";
            if (path.endsWith(".css")) return "text/css; charset=UTF-8";
            if (path.endsWith(".js")) return "application/javascript; charset=UTF-8";
            if (path.endsWith(".json")) return "application/json";
            if (path.endsWith(".png")) return "image/png";
            if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
            if (path.endsWith(".gif")) return "image/gif";
            if (path.endsWith(".svg")) return "image/svg+xml";
            if (path.endsWith(".ico")) return "image/x-icon";
            return "text/plain";
        }
    }

    // --- GET /api/questions - View all questions (Admin) ---
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

                byte[] response = jsonResponse.getBytes(StandardCharsets.UTF_8);
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

    // --- POST /api/questions/add - Add new question (Admin - Member 2) ---
    static class AddQuestionHandler implements HttpHandler {
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
                try {
                    InputStream is = exchange.getRequestBody();
                    String body = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                        .lines()
                        .collect(Collectors.joining("\n"));

                    Question question = new Gson().fromJson(body, Question.class);
                    
                    if (question.getQuestion() == null || question.getQuestion().trim().isEmpty()) {
                        sendResponse(exchange, 400, "{\"success\":false,\"message\":\"Question text is required\"}");
                        return;
                    }
                    if (question.getOptions() == null || question.getOptions().size() != 4) {
                        sendResponse(exchange, 400, "{\"success\":false,\"message\":\"Exactly 4 options are required\"}");
                        return;
                    }
                    if (question.getAnswer() == null || question.getAnswer().trim().isEmpty()) {
                        sendResponse(exchange, 400, "{\"success\":false,\"message\":\"Correct answer is required\"}");
                        return;
                    }

                    boolean success = QuestionManager.addQuestion(question);
                    
                    if (success) {
                        sendResponse(exchange, 200, "{\"success\":true,\"message\":\"Question added successfully\"}");
                    } else {
                        sendResponse(exchange, 500, "{\"success\":false,\"message\":\"Failed to add question\"}");
                    }

                } catch (Exception e) {
                    sendResponse(exchange, 400, "{\"success\":false,\"message\":\"Invalid request: " + e.getMessage() + "\"}");
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    // --- PUT /api/questions/update?id=X - Update question (Admin - Member 2) ---
    static class UpdateQuestionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "PUT, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("PUT".equals(exchange.getRequestMethod())) {
                try {
                    String query = exchange.getRequestURI().getQuery();
                    if (query == null || !query.startsWith("id=")) {
                        sendResponse(exchange, 400, "{\"success\":false,\"message\":\"Question ID is required\"}");
                        return;
                    }
                    int id = Integer.parseInt(query.substring(3));

                    InputStream is = exchange.getRequestBody();
                    String body = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                        .lines()
                        .collect(Collectors.joining("\n"));

                    Question question = new Gson().fromJson(body, Question.class);

                    if (question.getQuestion() == null || question.getQuestion().trim().isEmpty()) {
                        sendResponse(exchange, 400, "{\"success\":false,\"message\":\"Question text is required\"}");
                        return;
                    }
                    if (question.getOptions() == null || question.getOptions().size() != 4) {
                        sendResponse(exchange, 400, "{\"success\":false,\"message\":\"Exactly 4 options are required\"}");
                        return;
                    }
                    if (question.getAnswer() == null || question.getAnswer().trim().isEmpty()) {
                        sendResponse(exchange, 400, "{\"success\":false,\"message\":\"Correct answer is required\"}");
                        return;
                    }

                    boolean success = QuestionManager.updateQuestion(id, question);
                    
                    if (success) {
                        sendResponse(exchange, 200, "{\"success\":true,\"message\":\"Question updated successfully\"}");
                    } else {
                        sendResponse(exchange, 404, "{\"success\":false,\"message\":\"Question not found\"}");
                    }

                } catch (NumberFormatException e) {
                    sendResponse(exchange, 400, "{\"success\":false,\"message\":\"Invalid question ID\"}");
                } catch (Exception e) {
                    sendResponse(exchange, 400, "{\"success\":false,\"message\":\"Invalid request: " + e.getMessage() + "\"}");
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    // --- DELETE /api/questions/delete?id=X - Delete question (Admin - Member 2) ---
    static class DeleteQuestionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "DELETE, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("DELETE".equals(exchange.getRequestMethod())) {
                try {
                    String query = exchange.getRequestURI().getQuery();
                    if (query == null || !query.startsWith("id=")) {
                        sendResponse(exchange, 400, "{\"success\":false,\"message\":\"Question ID is required\"}");
                        return;
                    }
                    int id = Integer.parseInt(query.substring(3));

                    boolean success = QuestionManager.deleteQuestion(id);
                    
                    if (success) {
                        sendResponse(exchange, 200, "{\"success\":true,\"message\":\"Question deleted successfully\"}");
                    } else {
                        sendResponse(exchange, 404, "{\"success\":false,\"message\":\"Question not found\"}");
                    }

                } catch (NumberFormatException e) {
                    sendResponse(exchange, 400, "{\"success\":false,\"message\":\"Invalid question ID\"}");
                } catch (Exception e) {
                    sendResponse(exchange, 400, "{\"success\":false,\"message\":\"Invalid request: " + e.getMessage() + "\"}");
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(responseBytes);
        os.close();
    }

    // --- Handler for /api/students - Shows BOTH TCP and WebSocket clients ---
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
                List<ClientHandler> tcpClients = QuizServer.getConnectedClients();
                List<WebSocketClient> wsClients = QuizServer.getWebSocketClients();
                
                StringBuilder json = new StringBuilder("[");
                int id = 1;
                
                for (ClientHandler client : tcpClients) {
                    if (id > 1) json.append(",");
                    json.append("{")
                        .append("\"id\":").append(id++).append(",")
                        .append("\"name\":\"").append(client.getClientName()).append(" (TCP)\",")
                        .append("\"connectedAt\":\"").append(new java.util.Date()).append("\"")
                        .append("}");
                }
                
                for (WebSocketClient client : wsClients) {
                    if (id > 1) json.append(",");
                    json.append("{")
                        .append("\"id\":").append(id++).append(",")
                        .append("\"name\":\"").append(client.getClientName()).append(" (Web)\",")
                        .append("\"connectedAt\":\"").append(new java.util.Date()).append("\"")
                        .append("}");
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
                QuizServer.broadcastQuestions();

                String response = "{\"success\":true,\"message\":\"Quiz started\"}";
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
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("GET".equals(exchange.getRequestMethod())) {
                String response = "{\"status\":\"online\",\"port\":" + HTTP_PORT + ",\"timestamp\":" + System.currentTimeMillis() + "}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    // --- Member 4: GET /api/results - Get all student scores (Result Board) ---
    static class ResultsHandler implements HttpHandler {
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
                String jsonResponse = ResultDistributor.getResultBoard();
                
                byte[] response = jsonResponse.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length);
                OutputStream os = exchange.getResponseBody();
                os.write(response);
                os.close();
                
                // Also print to console
                ResultDistributor.printResultBoard();
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    // --- Member 4: GET /api/results/student?name=X - Get individual student result ---
    static class StudentResultHandler implements HttpHandler {
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
                try {
                    String query = exchange.getRequestURI().getQuery();
                    if (query == null || !query.startsWith("name=")) {
                        sendResponse(exchange, 400, "{\"success\":false,\"message\":\"Student name is required\"}");
                        return;
                    }
                    
                    String studentName = java.net.URLDecoder.decode(query.substring(5), "UTF-8");
                    String jsonResponse = ResultDistributor.getStudentResult(studentName);
                    
                    byte[] response = jsonResponse.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, response.length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(response);
                    os.close();
                    
                } catch (Exception e) {
                    sendResponse(exchange, 400, "{\"success\":false,\"message\":\"Invalid request: " + e.getMessage() + "\"}");
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    // --- Member 4: POST /api/evaluate - Evaluate answers and calculate scores ---
    static class EvaluateAnswersHandler implements HttpHandler {
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
                try {
                    InputStream is = exchange.getRequestBody();
                    String body = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                        .lines()
                        .collect(Collectors.joining("\n"));

                    // Expected JSON: {"studentName": "John", "answers": {"1": "A", "2": "B"}}
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> requestData = new Gson().fromJson(body, java.util.Map.class);
                    
                    String studentName = (String) requestData.get("studentName");
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, String> answersMap = (java.util.Map<String, String>) requestData.get("answers");
                    
                    if (studentName == null || answersMap == null) {
                        sendResponse(exchange, 400, "{\"success\":false,\"message\":\"Student name and answers are required\"}");
                        return;
                    }
                    
                    // Convert String keys to Integer for question IDs
                    java.util.Map<Integer, String> answers = new java.util.HashMap<>();
                    for (java.util.Map.Entry<String, String> entry : answersMap.entrySet()) {
                        answers.put(Integer.parseInt(entry.getKey()), entry.getValue());
                    }
                    
                    // Evaluate all answers
                    int totalScore = AnswerEvaluator.evaluateAllAnswers(studentName, answers);
                    int totalQuestions = AnswerEvaluator.getTotalQuestions();
                    java.util.Map<Integer, Boolean> detailedResults = AnswerEvaluator.getStudentResults(studentName);
                    
                    // Build detailed results JSON
                    Gson gson = new Gson();
                    String detailedResultsJson = gson.toJson(detailedResults);
                    
                    String response = "{\"success\":true,\"studentName\":\"" + studentName + 
                                    "\",\"score\":" + totalScore + 
                                    ",\"totalQuestions\":" + totalQuestions + 
                                    ",\"percentage\":" + (totalQuestions > 0 ? (totalScore * 100.0 / totalQuestions) : 0) + 
                                    ",\"detailedResults\":" + detailedResultsJson + "}";
                    
                    sendResponse(exchange, 200, response);

                } catch (Exception e) {
                    sendResponse(exchange, 400, "{\"success\":false,\"message\":\"Invalid request: " + e.getMessage() + "\"}");
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }
}