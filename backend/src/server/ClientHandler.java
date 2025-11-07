package server;

import java.io.*;
import java.net.*;
import server.questions.Question;
import java.util.List;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private ObjectOutputStream objectOutput;
    private ObjectInputStream objectInput;
    private String studentName;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            // Initialize streams - ObjectOutputStream must be created first
            objectOutput = new ObjectOutputStream(socket.getOutputStream());
            objectOutput.flush();
            objectInput = new ObjectInputStream(socket.getInputStream());
            
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);

            // Receive student name
            output.println("WELCOME");
            studentName = input.readLine();
            if (studentName == null || studentName.trim().isEmpty()) {
                studentName = "Student-" + socket.getPort();
            }
            System.out.println("🧑 Student joined: " + studentName);

            // Notify client
            output.println("Welcome, " + studentName + "! Please wait for the quiz to start...");

            // Keep connection open for later communication
            while (true) {
                String message = input.readLine();
                if (message == null || message.equalsIgnoreCase("QUIT")) break;
                System.out.println("[!] Connection lost with " + studentName);
            }

        } catch (IOException e) {
            System.out.println("⚠️ Connection lost with " + studentName);
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
        }
    }
    
    // Member 2: Send questions using ObjectOutputStream
    public void sendQuestions(List<Question> questions) {
        try {
            if (objectOutput != null) {
                objectOutput.writeObject(questions);
                objectOutput.flush();
                System.out.println("📤 Sent " + questions.size() + " questions to " + studentName);
            }
        } catch (IOException e) {
            System.out.println("⚠️ Error sending questions to " + studentName + ": " + e.getMessage());
        }
    }
    
    // Member 2: Send individual question using ObjectOutputStream
    public void sendQuestion(Question question) {
        try {
            if (objectOutput != null) {
                objectOutput.writeObject(question);
                objectOutput.flush();
                System.out.println("📤 Sent question to " + studentName);
            }
        } catch (IOException e) {
            System.out.println("⚠️ Error sending question to " + studentName + ": " + e.getMessage());
        }
    }
}
