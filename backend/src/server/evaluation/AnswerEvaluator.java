package server.evaluation;

import server.questions.Question;
import server.questions.QuestionManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Member 4 - Answer Evaluation & Scoring
 * Features:
 * - Thread-safe score calculation using synchronized blocks
 * - Concurrent score storage using ConcurrentHashMap
 * - Real-time evaluation of all student answers
 * - Generates detailed results with question-by-question breakdown
 */
public class AnswerEvaluator {
    
    // Thread-safe score storage
    private static final ConcurrentHashMap<String, StudentResult> studentResults = new ConcurrentHashMap<>();
    private static final Object evaluationLock = new Object();
    private static final String ANSWERS_FILE = "backend/resources/answers.json";
    
    /**
     * Represents a student's complete result
     */
    public static class StudentResult {
        private String studentName;
        private int totalQuestions;
        private int correctAnswers;
        private int wrongAnswers;
        private double score;
        private double percentage;
        private Map<Integer, QuestionResult> questionResults;
        private long submissionTime;
        
        public StudentResult(String studentName) {
            this.studentName = studentName;
            this.questionResults = new HashMap<>();
            this.submissionTime = System.currentTimeMillis();
        }
        
        // Getters
        public String getStudentName() { return studentName; }
        public int getTotalQuestions() { return totalQuestions; }
        public int getCorrectAnswers() { return correctAnswers; }
        public int getWrongAnswers() { return wrongAnswers; }
        public double getScore() { return score; }
        public double getPercentage() { return percentage; }
        public Map<Integer, QuestionResult> getQuestionResults() { return questionResults; }
        public long getSubmissionTime() { return submissionTime; }
        
        // Setters
        public void setTotalQuestions(int total) { this.totalQuestions = total; }
        public void setCorrectAnswers(int correct) { this.correctAnswers = correct; }
        public void setWrongAnswers(int wrong) { this.wrongAnswers = wrong; }
        public void setScore(double score) { this.score = score; }
        public void setPercentage(double percentage) { this.percentage = percentage; }
    }
    
    /**
     * Represents result for a single question
     */
    public static class QuestionResult {
        private int questionId;
        private String question;
        private String studentAnswer;
        private String correctAnswer;
        private boolean isCorrect;
        private int marks;
        
        public QuestionResult(int questionId, String question, String studentAnswer, 
                             String correctAnswer, boolean isCorrect, int marks) {
            this.questionId = questionId;
            this.question = question;
            this.studentAnswer = studentAnswer;
            this.correctAnswer = correctAnswer;
            this.isCorrect = isCorrect;
            this.marks = marks;
        }
        
        // Getters
        public int getQuestionId() { return questionId; }
        public String getQuestion() { return question; }
        public String getStudentAnswer() { return studentAnswer; }
        public String getCorrectAnswer() { return correctAnswer; }
        public boolean isCorrect() { return isCorrect; }
        public int getMarks() { return marks; }
    }
    
    /**
     * Evaluates all student answers with thread-safe operations
     */
    public static void evaluateAllStudents() {
        synchronized (evaluationLock) {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("📊 MEMBER 4: STARTING ANSWER EVALUATION & SCORING");
            System.out.println("=".repeat(60));
            
            // Load student answers from file
            Map<String, Map<Integer, String>> studentAnswers = loadStudentAnswers();
            
            if (studentAnswers == null || studentAnswers.isEmpty()) {
                System.out.println("⚠️  No student answers found!");
                System.out.println("=".repeat(60) + "\n");
                return;
            }
            
            // Get all questions
            List<Question> questions = QuestionManager.getAllQuestions();
            
            System.out.println("📋 Total Questions: " + questions.size());
            System.out.println("👥 Total Students: " + studentAnswers.size());
            System.out.println("-".repeat(60));
            
            // Evaluate each student
            int studentCount = 0;
            for (Map.Entry<String, Map<Integer, String>> entry : studentAnswers.entrySet()) {
                studentCount++;
                String studentName = entry.getKey();
                Map<Integer, String> answers = entry.getValue();
                
                System.out.println("\n🧑 Evaluating Student #" + studentCount + ": " + studentName);
                StudentResult result = evaluateStudent(studentName, answers, questions);
                
                // Thread-safe storage using ConcurrentHashMap
                studentResults.put(studentName, result);
                
                System.out.println("   ✅ Correct: " + result.getCorrectAnswers() + "/" + result.getTotalQuestions());
                System.out.println("   ❌ Wrong: " + result.getWrongAnswers());
                System.out.println("   📈 Score: " + String.format("%.2f", result.getScore()) + "/" + result.getTotalQuestions());
                System.out.println("   📊 Percentage: " + String.format("%.2f", result.getPercentage()) + "%");
            }
            
            System.out.println("\n" + "=".repeat(60));
            System.out.println("✅ EVALUATION COMPLETE - " + studentCount + " students evaluated");
            System.out.println("=".repeat(60) + "\n");
        }
    }
    
    /**
     * Evaluates a single student's answers (thread-safe)
     */
    private static StudentResult evaluateStudent(String studentName, 
                                                 Map<Integer, String> studentAnswers,
                                                 List<Question> questions) {
        StudentResult result = new StudentResult(studentName);
        result.setTotalQuestions(questions.size());
        
        int correctCount = 0;
        int wrongCount = 0;
        
        // Evaluate each question
        for (Question question : questions) {
            int questionId = question.getId();
            String studentAnswer = studentAnswers.get(questionId);
            String correctAnswerLetter = question.getAnswer(); // "A", "B", "C", "D"
            
            // Convert letter to actual answer text
            String correctAnswerText = getAnswerTextFromLetter(question, correctAnswerLetter);
            
            // Check if answer is correct (compare with actual text)
            boolean isCorrect = false;
            int marks = 0;
            
            if (studentAnswer != null && correctAnswerText != null) {
                isCorrect = studentAnswer.trim().equalsIgnoreCase(correctAnswerText.trim());
                marks = isCorrect ? 1 : 0;
            }
            
            if (isCorrect) {
                correctCount++;
            } else {
                wrongCount++;
            }
            
            // Store question result with actual answer text for display
            QuestionResult qResult = new QuestionResult(
                questionId,
                question.getQuestion(),
                studentAnswer != null ? studentAnswer : "No Answer",
                correctAnswerText != null ? correctAnswerText : correctAnswerLetter,
                isCorrect,
                marks
            );
            
            result.getQuestionResults().put(questionId, qResult);
        }
        
        // Calculate final scores (synchronized for thread safety)
        synchronized (result) {
            result.setCorrectAnswers(correctCount);
            result.setWrongAnswers(wrongCount);
            result.setScore(correctCount);
            result.setPercentage((correctCount * 100.0) / questions.size());
        }
        
        return result;
    }
    
    /**
     * Load student answers from JSON file
     */
    private static Map<String, Map<Integer, String>> loadStudentAnswers() {
        try {
            File file = new File(ANSWERS_FILE);
            if (!file.exists()) {
                return new HashMap<>();
            }
            
            BufferedReader reader = new BufferedReader(new FileReader(file));
            Type type = new TypeToken<Map<String, Map<Integer, String>>>(){}.getType();
            Map<String, Map<Integer, String>> answers = new Gson().fromJson(reader, type);
            reader.close();
            
            return answers != null ? answers : new HashMap<>();
        } catch (Exception e) {
            System.err.println("❌ Error loading answers: " + e.getMessage());
            return new HashMap<>();
        }
    }
    
    /**
     * Get all evaluated results (thread-safe)
     */
    public static Map<String, StudentResult> getAllResults() {
        return new HashMap<>(studentResults);
    }
    
    /**
     * Get specific student result (thread-safe)
     */
    public static StudentResult getStudentResult(String studentName) {
        return studentResults.get(studentName);
    }
    
    /**
     * Get sorted leaderboard (highest score first)
     */
    public static List<StudentResult> getLeaderboard() {
        List<StudentResult> leaderboard = new ArrayList<>(studentResults.values());
        
        // Sort by score (descending), then by submission time (ascending)
        leaderboard.sort((r1, r2) -> {
            int scoreCompare = Double.compare(r2.getScore(), r1.getScore());
            if (scoreCompare != 0) return scoreCompare;
            return Long.compare(r1.getSubmissionTime(), r2.getSubmissionTime());
        });
        
        return leaderboard;
    }
    
    /**
     * Clear all results (for new quiz session)
     */
    public static void clearResults() {
        synchronized (evaluationLock) {
            studentResults.clear();
            System.out.println("🗑️  All evaluation results cleared");
        }
    }
    
    /**
     * Get total number of evaluated students
     */
    public static int getTotalStudents() {
        return studentResults.size();
    }
    
    /**
     * Helper method to convert answer letter (A, B, C, D) to actual text
     */
    private static String getAnswerTextFromLetter(Question question, String letter) {
        if (letter == null || letter.isEmpty()) {
            return null;
        }
        
        List<String> options = question.getOptions();
        if (options == null || options.isEmpty()) {
            return null;
        }
        
        // Convert letter to index (A=0, B=1, C=2, D=3)
        int index = letter.toUpperCase().charAt(0) - 'A';
        
        if (index >= 0 && index < options.size()) {
            return options.get(index);
        }
        
        return null;
    }
}
