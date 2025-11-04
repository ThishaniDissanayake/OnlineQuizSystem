package server;

import java.io.*;
import java.net.*;

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

            // Receive student name
            output.println("Enter your name: ");
            studentName = input.readLine();
            System.out.println("🧑 Student joined: " + studentName);

            // Notify client
            output.println("Welcome, " + studentName + "! Please wait for the quiz to start...");

            // Keep connection open for later communication
            while (true) {
                String message = input.readLine();
                if (message == null || message.equalsIgnoreCase("exit")) break;
                System.out.println(studentName + ": " + message);
            }

        } catch (IOException e) {
            System.out.println("❌ Connection lost with " + studentName);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String getStudentName() {
        return studentName;
    }
}
