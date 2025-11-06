import { HttpClient } from "../api/httpClient.js";
import { ENDPOINTS } from "../api/endpoints.js";

export async function submitAnswers(questions) {
  const answers = [];

  questions.forEach((q) => {
    const selected = document.querySelector(`input[name="q${q.id}"]:checked`);
    answers.push({
      questionId: q.id,
      answer: selected ? selected.value : null,
    });
  });

  try {
    const res = await HttpClient.post(`${ENDPOINTS.START_QUIZ}/submit`, {
      answers,
    });
    alert("Answers submitted successfully!");
    console.log("Submission result:", res);
  } catch (err) {
    console.error("Submission failed:", err);
    alert("Error submitting answers. Please try again.");
  }
}
