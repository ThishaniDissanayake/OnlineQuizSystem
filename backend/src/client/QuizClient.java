package client;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class QuizClient {
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private Scanner scanner;

    public static void main(String[] args) {
        new QuizClient().startClient();
    }

    public void startClient() {
        try {
            socket = new Socket("localhost", 5000);
            System.out.println("✅ Connected to quiz server at localhost:5000");

            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);
            scanner = new Scanner(System.in);

            // Wait for welcome message
            String welcome = input.readLine();
            if (welcome != null) {
                System.out.println("📩 " + welcome);
            }

            // Send name
            System.out.print("Enter your name: ");
            String name = scanner.nextLine();
            output.println(name);

            // Listen for messages from server in a separate thread
            new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = input.readLine()) != null) {
                        System.out.println("🧠 " + serverMessage);
                    }
                } catch (IOException e) {
                    System.out.println("🔌 Disconnected from server.");
                }
            }).start();

            // Keep client alive, optional input loop
            while (true) {
                String userInput = scanner.nextLine();
                if (userInput.equalsIgnoreCase("quit")) {
                    output.println("QUIT");
                    break;
                } else {
                    output.println(userInput);
                }
            }

            closeConnection();

        } catch (IOException e) {
            System.out.println("❌ Connection error: " + e.getMessage());
        }
    }

    private void closeConnection() {
        try {
            if (socket != null) socket.close();
            System.out.println("🔒 Connection closed.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
