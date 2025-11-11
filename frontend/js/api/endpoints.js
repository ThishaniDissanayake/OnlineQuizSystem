const API_BASE = "http://localhost:8080";

export const ENDPOINTS = {
  // Member 1 APIs
  STATUS: `${API_BASE}/api/status`,
  STUDENTS: `${API_BASE}/api/students`,
  START_QUIZ: `${API_BASE}/api/quiz/start`,

  // Member 2: Question Management APIs
  QUESTIONS: `${API_BASE}/api/questions`,
  ADD_QUESTION: `${API_BASE}/api/questions/add`,
  UPDATE_QUESTION: `${API_BASE}/api/questions/update`,
  DELETE_QUESTION: `${API_BASE}/api/questions/delete`,
  
  // Member 4 & 5: Evaluation and Results APIs
  EVALUATE_QUIZ: `${API_BASE}/api/quiz/evaluate`,
  LEADERBOARD: `${API_BASE}/api/results/leaderboard`,
  STUDENT_RESULT: `${API_BASE}/api/results/student`,
};