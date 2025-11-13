package server.questions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class QuestionManager {
    private static List<Question> questions = new ArrayList<>();
    private static final String QUESTIONS_FILE = "backend/resources/questions.json";

    // Load questions from JSON file (called at server startup)
    public static void loadQuestions() {
        try {
            File file = new File(QUESTIONS_FILE);
            System.out.println("📂 Loading questions from: " + file.getAbsolutePath());
            
            if (!file.exists()) {
                System.out.println("[!] questions.json not found. Creating new file...");
                file.getParentFile().mkdirs();
                file.createNewFile();
                questions = new ArrayList<>();
                saveQuestions(); // Save empty list
                return;
            }

            BufferedReader reader = new BufferedReader(new FileReader(file));
            Type questionListType = new TypeToken<List<Question>>() {}.getType();
            questions = new Gson().fromJson(reader, questionListType);
            reader.close();

            if (questions == null) {
                questions = new ArrayList<>();
            }

            System.out.println("✅ Loaded " + questions.size() + " questions from JSON.");
        } catch (Exception e) {
            System.out.println("[!] Error loading questions: " + e.getMessage());
            questions = new ArrayList<>();
        }
    }

    // Save questions to JSON file
    public static synchronized void saveQuestions() {
        try {
            File file = new File(QUESTIONS_FILE);
            file.getParentFile().mkdirs();
            
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            FileWriter writer = new FileWriter(file);
            gson.toJson(questions, writer);
            writer.close();
            
            System.out.println("💾 Saved " + questions.size() + " questions to JSON.");
        } catch (Exception e) {
            System.out.println("[!] Error saving questions: " + e.getMessage());
        }
    }

    // Get all questions
    public static List<Question> getAllQuestions() {
        return new ArrayList<>(questions); // Return copy to prevent external modification
    }

    // Add new question (for Admin - Member 2)
    public static synchronized boolean addQuestion(Question question) {
        try {
            // Auto-generate ID
            int newId = questions.isEmpty() ? 1 : questions.get(questions.size() - 1).getId() + 1;
            Question newQuestion = new Question(newId, question.getQuestion(), 
                                               question.getOptions(), question.getAnswer());
            questions.add(newQuestion);
            saveQuestions();
            System.out.println("➕ Added question ID: " + newId);
            return true;
        } catch (Exception e) {
            System.out.println("[!] Error adding question: " + e.getMessage());
            return false;
        }
    }

    // Update existing question (for Admin - Member 2)
    public static synchronized boolean updateQuestion(int id, Question updatedQuestion) {
        try {
            for (int i = 0; i < questions.size(); i++) {
                if (questions.get(i).getId() == id) {
                    Question newQuestion = new Question(id, updatedQuestion.getQuestion(),
                                                       updatedQuestion.getOptions(), 
                                                       updatedQuestion.getAnswer());
                    questions.set(i, newQuestion);
                    saveQuestions();
                    System.out.println("✏️ Updated question ID: " + id);
                    return true;
                }
            }
            System.out.println("[!] Question ID " + id + " not found.");
            return false;
        } catch (Exception e) {
            System.out.println("[!] Error updating question: " + e.getMessage());
            return false;
        }
    }

    // Delete question (for Admin - Member 2)
    public static synchronized boolean deleteQuestion(int id) {
        try {
            boolean removed = questions.removeIf(q -> q.getId() == id);
            if (removed) {
                saveQuestions();
                System.out.println("🗑️ Deleted question ID: " + id);
                return true;
            } else {
                System.out.println("[!] Question ID " + id + " not found.");
                return false;
            }
        } catch (Exception e) {
            System.out.println("[!] Error deleting question: " + e.getMessage());
            return false;
        }
    }

    // Get question by ID
    public static Question getQuestionById(int id) {
        for (Question q : questions) {
            if (q.getId() == id) {
                return q;
            }
        }
        return null;
    }
}