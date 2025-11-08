// Admin Dashboard JavaScript - Member 1 + Member 2
import { HttpClient } from "../api/httpClient.js";

let serverConnected = false;
let connectedStudents = [];
let questions = [];
let pollingInterval;
let editingQuestionId = null;

// DOM Elements
const statusIndicator = document.getElementById("status-indicator");
const statusText = document.getElementById("status-text");
const studentCount = document.getElementById("student-count");
const questionCount = document.getElementById("question-count");
const studentsList = document.getElementById("students-list");
const questionsList = document.getElementById("questions-list");
const activityLog = document.getElementById("activity-log");
const startQuizBtn = document.getElementById("start-quiz-btn");
const refreshBtn = document.getElementById("refresh-btn");
const addQuestionBtn = document.getElementById("add-question-btn");

// Modal elements
const modal = document.getElementById("question-modal");
const modalTitle = document.getElementById("modal-title");
const questionForm = document.getElementById("question-form");
const closeModal = document.querySelector(".close");
const cancelBtn = document.getElementById("cancel-btn");

// Initialize
document.addEventListener("DOMContentLoaded", () => {
  addLog("System", "Admin Dashboard initialized");
  checkServerConnection();
  setupEventListeners();
});

// Setup event listeners
function setupEventListeners() {
  startQuizBtn.addEventListener("click", startQuiz);
  refreshBtn.addEventListener("click", refreshAll);
  addQuestionBtn.addEventListener("click", openAddQuestionModal);
  closeModal.addEventListener("click", closeQuestionModal);
  cancelBtn.addEventListener("click", closeQuestionModal);
  questionForm.addEventListener("submit", saveQuestion);

  // Close modal when clicking outside
  window.addEventListener("click", (e) => {
    if (e.target === modal) {
      closeQuestionModal();
    }
  });
}

// Check server connection
function checkServerConnection() {
  HttpClient.checkServerStatus()
    .then((data) => {
      serverConnected = true;
      updateServerStatus(true);
      addLog("Server", `Connected to Quiz Server on port ${data.port}`);
      startPolling();
      loadQuestions();
    })
    .catch((error) => {
      serverConnected = false;
      updateServerStatus(false);
      addLog("Error", "Failed to connect to server. Make sure QuizServer is running.");
      setTimeout(checkServerConnection, 5000);
    });
}

// Update server status UI
function updateServerStatus(isOnline) {
  if (isOnline) {
    statusIndicator.classList.remove("offline");
    statusIndicator.classList.add("online");
    statusText.textContent = "Server Online";
    statusText.style.color = "#10b981";
  } else {
    statusIndicator.classList.remove("online");
    statusIndicator.classList.add("offline");
    statusText.textContent = "Server Offline";
    statusText.style.color = "#ef4444";
  }
}

// Start polling
function startPolling() {
  pollingInterval = setInterval(refreshStudentsList, 3000);
  refreshStudentsList();
}

// Refresh all data
function refreshAll() {
  refreshStudentsList();
  loadQuestions();
  addLog("System", "Refreshed all data");
}

// Refresh students list
function refreshStudentsList() {
  HttpClient.getConnectedStudents()
    .then((students) => {
      if (students.length > connectedStudents.length) {
        const newCount = students.length - connectedStudents.length;
        addLog("System", `${newCount} new student(s) connected`);
      }
      connectedStudents = students;
      updateStudentsList(students);
    })
    .catch((error) => {
      console.error("Error fetching students:", error);
    });
}

// Update students list UI
function updateStudentsList(students) {
  studentCount.textContent = students.length;
  startQuizBtn.disabled = students.length === 0 || questions.length === 0;

  if (students.length === 0) {
    studentsList.innerHTML = `
      <div class="empty-state">
        <p>🔭 No students connected yet</p>
        <p class="hint">Students will appear here when they connect</p>
      </div>
    `;
    return;
  }

  studentsList.innerHTML = students
    .map(
      (student) => `
        <div class="student-item">
          <div class="student-info">
            <div class="student-avatar">${student.name.charAt(0).toUpperCase()}</div>
            <div>
              <div class="student-name">${student.name}</div>
              <div class="student-status">● Connected</div>
            </div>
          </div>
        </div>
      `
    )
    .join("");
}

// ===== MEMBER 2: Question Management Functions =====

// Load questions from server
async function loadQuestions() {
  try {
    questions = await HttpClient.getQuestions();
    questionCount.textContent = questions.length;
    displayQuestions(questions);
    addLog("System", `Loaded ${questions.length} questions`);
    
    // Update start button state
    startQuizBtn.disabled = connectedStudents.length === 0 || questions.length === 0;
  } catch (error) {
    console.error("Error loading questions:", error);
    questionsList.innerHTML = '<p class="error">Failed to load questions</p>';
  }
}

// Display questions in the list
function displayQuestions(questions) {
  if (questions.length === 0) {
    questionsList.innerHTML = `
      <div class="empty-state">
        <p>📝 No questions added yet</p>
        <p class="hint">Click "Add New Question" to create your first question</p>
      </div>
    `;
    return;
  }

  questionsList.innerHTML = questions
    .map(
      (q) => `
        <div class="question-item" data-id="${q.id}">
          <div class="question-header">
            <span class="question-number">Q${q.id}</span>
            <span class="question-text">${escapeHtml(q.question)}</span>
          </div>
          <div class="question-options">
            ${q.options
              .map(
                (opt, idx) =>
                  `<span class="option ${opt === q.answer ? "correct" : ""}">${String.fromCharCode(65 + idx)}. ${escapeHtml(opt)}</span>`
              )
              .join("")}
          </div>
          <div class="question-actions">
            <button class="btn-icon edit-btn" onclick="window.editQuestion(${q.id})" title="Edit">✏️</button>
            <button class="btn-icon delete-btn" onclick="window.deleteQuestion(${q.id})" title="Delete">🗑️</button>
          </div>
        </div>
      `
    )
    .join("");
}

// Helper function to escape HTML
function escapeHtml(text) {
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}

// Open add question modal
function openAddQuestionModal() {
  editingQuestionId = null;
  modalTitle.textContent = "Add New Question";
  questionForm.reset();
  document.getElementById("question-id").value = "";
  modal.style.display = "block";
}

// Open edit question modal
window.editQuestion = function (id) {
  const question = questions.find((q) => q.id === id);
  if (!question) return;

  editingQuestionId = id;
  modalTitle.textContent = "Edit Question";
  document.getElementById("question-id").value = id;
  document.getElementById("question-text").value = question.question;
  document.getElementById("option-a").value = question.options[0] || "";
  document.getElementById("option-b").value = question.options[1] || "";
  document.getElementById("option-c").value = question.options[2] || "";
  document.getElementById("option-d").value = question.options[3] || "";

  // Find which option matches the answer
  const answerIndex = question.options.indexOf(question.answer);
  if (answerIndex !== -1) {
    document.getElementById("correct-answer").value = String.fromCharCode(65 + answerIndex);
  }

  modal.style.display = "block";
};

// Close modal
function closeQuestionModal() {
  modal.style.display = "none";
  questionForm.reset();
  editingQuestionId = null;
}

// Save question (add or update)
async function saveQuestion(e) {
  e.preventDefault();

  const questionText = document.getElementById("question-text").value.trim();
  const optionA = document.getElementById("option-a").value.trim();
  const optionB = document.getElementById("option-b").value.trim();
  const optionC = document.getElementById("option-c").value.trim();
  const optionD = document.getElementById("option-d").value.trim();
  const correctAnswerLetter = document.getElementById("correct-answer").value;

  if (!questionText || !optionA || !optionB || !optionC || !optionD || !correctAnswerLetter) {
    alert("Please fill in all fields!");
    return;
  }

  const options = [optionA, optionB, optionC, optionD];
  const correctAnswerIndex = correctAnswerLetter.charCodeAt(0) - 65; // A=0, B=1, C=2, D=3
  const answer = options[correctAnswerIndex];

  const questionData = {
    question: questionText,
    options: options,
    answer: answer,
  };

  try {
    if (editingQuestionId) {
      // Update existing question
      await HttpClient.updateQuestion(editingQuestionId, questionData);
      addLog("Admin", `Question ${editingQuestionId} updated`);
    } else {
      // Add new question
      await HttpClient.addQuestion(questionData);
      addLog("Admin", "New question added");
    }

    closeQuestionModal();
    await loadQuestions();
  } catch (error) {
    console.error("Error saving question:", error);
    alert("Failed to save question. Please try again.");
  }
}

// Delete question
window.deleteQuestion = async function (id) {
  if (!confirm(`Are you sure you want to delete Question ${id}?`)) {
    return;
  }

  try {
    await HttpClient.deleteQuestion(id);
    addLog("Admin", `Question ${id} deleted`);
    await loadQuestions();
  } catch (error) {
    console.error("Error deleting question:", error);
    alert("Failed to delete question. Please try again.");
  }
};

// ===== MEMBER 1: Start Quiz Function =====
function startQuiz() {
  const studentCountNum = connectedStudents.length;
  const questionCountNum = questions.length;

  if (studentCountNum === 0) {
    alert("No students connected!");
    return;
  }

  if (questionCountNum === 0) {
    alert("No questions available! Please add questions first.");
    return;
  }

  if (!confirm(`Start quiz with ${questionCountNum} questions for ${studentCountNum} students?`)) {
    return;
  }

  HttpClient.startQuiz()
    .then((data) => {
      addLog("Admin", `Quiz started! ${data.studentCount} students, ${questionCountNum} questions`);
      alert(`✅ Quiz started successfully!\n\nStudents: ${data.studentCount}\nQuestions: ${questionCountNum}\n\nQuestions have been broadcast to all connected students.`);
    })
    .catch((error) => {
      console.error("Error starting quiz:", error);
      addLog("Error", "Failed to start quiz");
      alert("❌ Failed to start quiz. Check console for errors.");
    });
}

// Add log entry
function addLog(source, message) {
  const timestamp = new Date().toLocaleTimeString();
  const logEntry = document.createElement("div");
  logEntry.className = "log-entry";
  logEntry.innerHTML = `
    <span class="timestamp">[${timestamp} - ${source}]</span>
    <span>${message}</span>
  `;
  activityLog.insertBefore(logEntry, activityLog.firstChild);

  if (activityLog.children.length > 50) {
    activityLog.removeChild(activityLog.lastChild);
  }
}

// Cleanup
window.addEventListener("beforeunload", () => {
  if (pollingInterval) {
    clearInterval(pollingInterval);
  }
});