import { loadQuestions } from "./loadQuestions.js";
import { submitAnswers } from "./submitAnswers.js";

document.addEventListener("DOMContentLoaded", async () => {
  const container = document.getElementById("question-panel");
  const submitBtn = document.getElementById("submit-btn");

  // Load questions
  const questions = await loadQuestions(container);

  // Handle answer submission
  submitBtn.addEventListener("click", () => {
    submitAnswers(questions);
  });
});
