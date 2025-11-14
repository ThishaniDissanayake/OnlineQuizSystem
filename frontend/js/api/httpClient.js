import { ENDPOINTS } from "./endpoints.js";

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

  static async put(url, data = {}) {
    try {
      const response = await fetch(url, {
        method: "PUT",
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
      console.error("PUT request failed:", error);
      throw error;
    }
  }

  static async delete(url) {
    try {
      const response = await fetch(url, {
        method: "DELETE",
      });
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      return await response.json();
    } catch (error) {
      console.error("DELETE request failed:", error);
      throw error;
    }
  }

  // Member 1 APIs
  static async checkServerStatus() {
    return await this.get(ENDPOINTS.STATUS);
  }

  static async getConnectedStudents() {
    return await this.get(ENDPOINTS.STUDENTS);
  }

  static async startQuiz() {
    return await this.post(ENDPOINTS.START_QUIZ);
  }

  // Member 2: Question Management APIs
  static async getQuestions() {
    return await this.get(ENDPOINTS.QUESTIONS);
  }

  static async addQuestion(questionData) {
    return await this.post(ENDPOINTS.ADD_QUESTION, questionData);
  }

  static async updateQuestion(id, questionData) {
    return await this.put(`${ENDPOINTS.UPDATE_QUESTION}?id=${id}`, questionData);
  }

  static async deleteQuestion(id) {
    return await this.delete(`${ENDPOINTS.DELETE_QUESTION}?id=${id}`);
  }
}

// Convenience function exports used by some modules
export async function httpGet(url) { return HttpClient.get(url); }
export async function httpPost(url, data = {}) { return HttpClient.post(url, data); }
export async function httpPut(url, data = {}) { return HttpClient.put(url, data); }
export async function httpDelete(url) { return HttpClient.delete(url); }