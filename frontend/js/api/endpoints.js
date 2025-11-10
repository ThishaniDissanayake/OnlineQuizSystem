const API_BASE = "http://localhost:8080";

export const API_ENDPOINTS = {
  // Member 1 APIs
  STATUS: `${API_BASE}/api/status`,
  STUDENTS: `${API_BASE}/api/students`,
  START_QUIZ: `${API_BASE}/api/quiz/start`,

  // Member 2: Question Management APIs
  QUESTIONS: `${API_BASE}/api/questions`,
  ADD_QUESTION: `${API_BASE}/api/questions/add`,
  UPDATE_QUESTION: `${API_BASE}/api/questions/update`,
  DELETE_QUESTION: `${API_BASE}/api/questions/delete`,

  // Member 4: Answer Evaluation & Scoring APIs
  RESULTS: `${API_BASE}/api/results`,
  STUDENT_RESULT: (studentName) => `${API_BASE}/api/results/student?name=${encodeURIComponent(studentName)}`,
  EVALUATE: `${API_BASE}/api/evaluate`,
};

// Keep backward compatibility
export const ENDPOINTS = API_ENDPOINTS;