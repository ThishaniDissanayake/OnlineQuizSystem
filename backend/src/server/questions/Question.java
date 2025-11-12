package server.questions;

import java.io.Serializable;
import java.util.List;

public class Question implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int id;
    private String question;
    private List<String> options;
    private String answer;

    public Question(int id, String question, List<String> options, String answer) {
        this.id = id;
        this.question = question;
        this.options = options;
        this.answer = answer;
    }

    public int getId() { return id; }
    public String getQuestion() { return question; }
    public List<String> getOptions() { return options; }
    public String getAnswer() { return answer; }

    @Override
    public String toString() {
        return "Question ID: " + id + " | " + question + " | Options: " + options;
    }
}
