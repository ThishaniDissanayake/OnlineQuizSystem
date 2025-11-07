package server.questions;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class QuestionManager {
    private static List<Question> questions = new ArrayList<>();

    public static void loadQuestions() {
        try {
            File file = new File("resources/questions.json");
             System.out.println("📂 Loading questions from: " + file.getAbsolutePath());
            if (!file.exists()) {
                System.out.println("[!] questions.json not found at " + file.getAbsolutePath());
                return;
            }

            BufferedReader reader = new BufferedReader(new FileReader(file));
            Type questionListType = new TypeToken<List<Question>>() {}.getType();
            questions = new Gson().fromJson(reader, questionListType);
            reader.close();

            System.out.println("✅ Loaded " + questions.size() + " questions from JSON.");
        } catch (Exception e) {
            System.out.println("[!] Error loading questions: " + e.getMessage());
        }
    }

    public static List<Question> getAllQuestions() {
        return questions;
    }
}
