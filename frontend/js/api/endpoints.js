// API endpoints configuration
const API_BASE = "http://localhost:8080";

export const ENDPOINTS = {
  STATUS: `${API_BASE}/api/status`,
  STUDENTS: `${API_BASE}/api/students`,
  START_QUIZ: `${API_BASE}/api/quiz/start`,

  // ===  QUESTION APIs ===
  QUESTIONS: `${API_BASE}/api/questions`, 
  SUBMIT_ANSWERS: `${API_BASE}/api/quiz/submit`, 
};
