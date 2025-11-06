import { ENDPOINTS } from "./endpoints.js";

// HTTP Client utility for API calls
export class HttpClient {
  static async get(url) {
    try {
      const response = await fetch(url);
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      return await response.json();
    } catch (error) {
      console.error("GET request failed:", error);
      throw error;
    }
  }

  static async post(url, data = {}) {
    try {
      const response = await fetch(url, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(data),
      });
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      return await response.json();
    } catch (error) {
      console.error("POST request failed:", error);
      throw error;
    }
  }

  // Specific API methods
  static async checkServerStatus() {
    return await this.get(ENDPOINTS.STATUS);
  }

  static async getConnectedStudents() {
    return await this.get(ENDPOINTS.STUDENTS);
  }

  static async startQuiz() {
    return await this.post(ENDPOINTS.START_QUIZ);
  }

  // === NEW QUESTION APIs ===
  static async getQuestions() {
    return await this.get(ENDPOINTS.QUESTIONS);
  }

  static async submitAnswers(answers) {
    return await this.post(ENDPOINTS.SUBMIT_ANSWERS, { answers });
  }
}
