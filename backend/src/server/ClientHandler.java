package server;

import java.io.*;
import java.net.*;
import server.questions.Question;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private String studentName;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);

            output.println("WELCOME");

            studentName = input.readLine();
            if (studentName == null || studentName.trim().isEmpty()) {
                studentName = "Student-" + socket.getPort();
            }
            System.out.println("🧑 Student joined: " + studentName);

            output.println("Welcome, " + studentName + "! Please wait for the quiz to start...");

            String message;
            while ((message = input.readLine()) != null) {
                if (message.equalsIgnoreCase("QUIT")) {
                    System.out.println("👋 " + studentName + " has left the quiz.");
                    break;
                }
                System.out.println("💬 " + studentName + ": " + message);
            }

        } catch (IOException e) {
            System.out.println("❌ Connection lost with " + studentName);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            QuizServer.removeClient(this);
        }
    }

    public String getStudentName() {
        return studentName;
    }

    public String getClientName() {
        return studentName != null ? studentName : "Unknown";
    }

    public void send(String message) {
        if (output != null) {
            output.println(message);
            output.flush();
        }
    }

    public void sendQuestion(Question question) {
        if (output != null && question != null) {
            String formatted = "QUESTION|" + question.getId() + "|" + question.getQuestion()
                    + "|" + String.join(",", question.getOptions());
            output.println(formatted);
            output.flush();
        }
    }
}