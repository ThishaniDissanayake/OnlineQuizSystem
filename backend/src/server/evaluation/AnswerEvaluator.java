package server.evaluation;

import server.questions.Question;
import server.questions.QuestionManager;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Member 4 - Answer Evaluation & Scoring
 * Evaluates answers from all clients and calculates total marks
 * Uses synchronized blocks for thread-safe score updates
 */
public class AnswerEvaluator {
    
    // Thread-safe storage for student scores
    private static final Map<String, Integer> studentScores = new HashMap<>();
    private static final Map<String, Map<Integer, Boolean>> studentResults = new HashMap<>();
    
    /**
     * Evaluate a single answer for a student
     * @param studentName Name of the student
     * @param questionId Question ID
     * @param studentAnswer Student's answer
     * @return true if correct, false otherwise
     */
    public static synchronized boolean evaluateAnswer(String studentName, int questionId, String studentAnswer) {
        Question question = QuestionManager.getQuestionById(questionId);
        
        if (question == null || studentAnswer == null) {
            System.out.println("✗ Question " + questionId + " not found or answer is null");
            return false;
        }
        
        // Get the correct answer from the question
        String correctAnswer = question.getAnswer();
        
        // Compare answer (case-insensitive and trim whitespace)
        boolean isCorrect = correctAnswer.trim().equalsIgnoreCase(studentAnswer.trim());
        
        // Store result
        studentResults
            .computeIfAbsent(studentName, k -> new HashMap<>())
            .put(questionId, isCorrect);
        
        // Update score if correct
        if (isCorrect) {
            updateScore(studentName, 1); // 1 mark per correct answer
        }
        
        System.out.println("✓ Evaluated: " + studentName + " Q" + questionId + 
                         " | Student: '" + studentAnswer + "' | Correct: '" + correctAnswer + "' | " +
                         (isCorrect ? "CORRECT ✓" : "WRONG ✗"));
        
        return isCorrect;
    }
    
    /**
     * Evaluate all answers for a student
     * @param studentName Name of the student
     * @param answers Map of questionId -> studentAnswer
     * @return Total score
     */
    public static synchronized int evaluateAllAnswers(String studentName, Map<Integer, String> answers) {
        // Reset this student's score before evaluating
        studentScores.put(studentName, 0);
        studentResults.put(studentName, new HashMap<>());
        
        int score = 0;
        
        for (Map.Entry<Integer, String> entry : answers.entrySet()) {
            if (evaluateAnswer(studentName, entry.getKey(), entry.getValue())) {
                score++;
            }
        }
        
        System.out.println("📊 Final Score for " + studentName + ": " + score + "/" + answers.size());
        return score;
    }
    
    /**
     * Thread-safe score update
     * @param studentName Name of the student
     * @param marksToAdd Marks to add
     */
    private static synchronized void updateScore(String studentName, int marksToAdd) {
        studentScores.put(studentName, studentScores.getOrDefault(studentName, 0) + marksToAdd);
    }
    
    /**
     * Get total marks for a student
     * @param studentName Name of the student
     * @return Total marks
     */
    public static synchronized int getTotalMarks(String studentName) {
        return studentScores.getOrDefault(studentName, 0);
    }
    
    /**
     * Get all student scores
     * @return Map of studentName -> totalScore
     */
    public static synchronized Map<String, Integer> getAllScores() {
        return new HashMap<>(studentScores); // Return copy for thread safety
    }
    
    /**
     * Get detailed results for a student
     * @param studentName Name of the student
     * @return Map of questionId -> isCorrect
     */
    public static synchronized Map<Integer, Boolean> getStudentResults(String studentName) {
        return new HashMap<>(studentResults.getOrDefault(studentName, new HashMap<>()));
    }
    
    /**
     * Reset all scores and results (for new quiz)
     */
    public static synchronized void resetAll() {
        studentScores.clear();
        studentResults.clear();
        System.out.println("🔄 All scores and results reset");
    }
    
    /**
     * Get total number of questions
     * @return Total questions
     */
    public static int getTotalQuestions() {
        return QuestionManager.getAllQuestions().size();
    }
    
    /**
     * Get student result details
     * @param studentName Name of the student
     * @return StudentResult object or null
     */
    public static synchronized StudentResult getStudentResult(String studentName) {
        if (!studentScores.containsKey(studentName)) {
            return null;
        }
        
        int score = studentScores.get(studentName);
        int total = getTotalQuestions();
        double percentage = total > 0 ? (score * 100.0 / total) : 0;
        
        return new StudentResult(studentName, score, total, percentage);
    }
    
    /**
     * Get leaderboard (all students sorted by score)
     * @return List of StudentResult sorted by score descending
     */
    public static synchronized List<StudentResult> getLeaderboard() {
        int totalQuestions = getTotalQuestions();
        
        return studentScores.entrySet().stream()
            .map(entry -> {
                String name = entry.getKey();
                int score = entry.getValue();
                double percentage = totalQuestions > 0 ? (score * 100.0 / totalQuestions) : 0;
                return new StudentResult(name, score, totalQuestions, percentage);
            })
            .sorted((a, b) -> Integer.compare(b.score, a.score)) // Sort by score descending
            .collect(Collectors.toList());
    }
    
    /**
     * Inner class to represent student result
     */
    public static class StudentResult {
        public String studentName;
        public int score;
        public int totalQuestions;
        public double percentage;
        
        public StudentResult(String studentName, int score, int totalQuestions, double percentage) {
            this.studentName = studentName;
            this.score = score;
            this.totalQuestions = totalQuestions;
            this.percentage = percentage;
        }
    }
}
