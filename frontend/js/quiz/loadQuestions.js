import { HttpClient } from "../api/httpClient.js";
import { ENDPOINTS } from "../api/endpoints.js";
import { createQuestionCard } from "./uiHelpers.js";

export async function loadQuestions(container) {
  try {
    const data = await HttpClient.get(`${ENDPOINTS.QUESTIONS}/questions`);
    container.innerHTML = "";

    if (!data || data.length === 0) {
      container.innerHTML = "<p>No questions available.</p>";
      return [];
    }

    data.forEach((q, i) => {
      const questionEl = createQuestionCard(q, i + 1);
      container.appendChild(questionEl);
    });

    return data;
  } catch (err) {
    console.error("Failed to load questions:", err);
    container.innerHTML = "<p>Error loading questions. Try again later.</p>";
    return [];
  }
}
