package server.results;

import server.evaluation.AnswerEvaluator;
import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;

/**
 * Member 4 - Result Distribution
 * Distributes final scores to students and displays result board
 */
public class ResultDistributor {
    
    /**
     * Get result board with all student scores
     * @return JSON string with all results
     */
    public static synchronized String getResultBoard() {
        Map<String, Integer> scores = AnswerEvaluator.getAllScores();
        int totalQuestions = AnswerEvaluator.getTotalQuestions();
        
        Map<String, Object> resultBoard = new HashMap<>();
        resultBoard.put("totalQuestions", totalQuestions);
        resultBoard.put("scores", scores);
        resultBoard.put("timestamp", System.currentTimeMillis());
        
        return new Gson().toJson(resultBoard);
    }
    
    /**
     * Get individual student result
     * @param studentName Name of the student
     * @return JSON string with student result
     */
    public static synchronized String getStudentResult(String studentName) {
        int score = AnswerEvaluator.getTotalMarks(studentName);
        Map<Integer, Boolean> detailedResults = AnswerEvaluator.getStudentResults(studentName);
        int totalQuestions = AnswerEvaluator.getTotalQuestions();
        
        Map<String, Object> result = new HashMap<>();
        result.put("studentName", studentName);
        result.put("score", score);
        result.put("totalQuestions", totalQuestions);
        result.put("percentage", totalQuestions > 0 ? (score * 100.0 / totalQuestions) : 0);
        result.put("detailedResults", detailedResults);
        result.put("timestamp", System.currentTimeMillis());
        
        return new Gson().toJson(result);
    }
    
    /**
     * Distribute results to all students (broadcast)
     * @return Map of studentName -> result JSON
     */
    public static synchronized Map<String, String> distributeResultsToAll() {
        Map<String, Integer> scores = AnswerEvaluator.getAllScores();
        Map<String, String> results = new HashMap<>();
        
        for (String studentName : scores.keySet()) {
            results.put(studentName, getStudentResult(studentName));
        }
        
        System.out.println("📢 Results distributed to " + results.size() + " students");
        return results;
    }
    
    /**
     * Print result board to console
     */
    public static synchronized void printResultBoard() {
        Map<String, Integer> scores = AnswerEvaluator.getAllScores();
        int totalQuestions = AnswerEvaluator.getTotalQuestions();
        
        System.out.println("\n" + "=".repeat(50));
        System.out.println("📊 QUIZ RESULTS - LEADERBOARD");
        System.out.println("=".repeat(50));
        System.out.println(String.format("%-20s | %-10s | %-10s", "Student Name", "Score", "Percentage"));
        System.out.println("-".repeat(50));
        
        scores.entrySet().stream()
            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue())) // Sort by score descending
            .forEach(entry -> {
                String name = entry.getKey();
                int score = entry.getValue();
                double percentage = totalQuestions > 0 ? (score * 100.0 / totalQuestions) : 0;
                System.out.println(String.format("%-20s | %-10s | %.2f%%", 
                    name, score + "/" + totalQuestions, percentage));
            });
        
        System.out.println("=".repeat(50) + "\n");
    }
}
