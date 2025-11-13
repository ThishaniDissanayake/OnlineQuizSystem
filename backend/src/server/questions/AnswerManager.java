package server.questions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class AnswerManager {

    private static final String ANSWERS_FILE = "backend/resources/answers.json";
    private static Map<String, Map<Integer, String>> studentAnswers = new HashMap<>();

    // Record an answer
    public static synchronized void recordAnswer(String studentName, int questionId, String answer) {
        studentAnswers
            .computeIfAbsent(studentName, k -> new HashMap<>())
            .put(questionId, answer);

        saveAnswers();
    }

    // Save to JSON
    private static synchronized void saveAnswers() {
        try (FileWriter writer = new FileWriter(ANSWERS_FILE)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(studentAnswers, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
