// Questions Management Page JavaScript
import { HttpClient } from "../api/httpClient.js";

let questions = [];
let editingQuestionId = null;

// DOM Elements
const statusIndicator = document.getElementById("status-indicator");
const statusText = document.getElementById("status-text");
const serverStatusText = document.getElementById("server-status-text");
const totalQuestionsEl = document.getElementById("total-questions");
const questionsList = document.getElementById("questions-list");
const addQuestionBtn = document.getElementById("add-question-btn");

// Modal elements
const modal = document.getElementById("question-modal");
const modalTitle = document.getElementById("modal-title");
const questionForm = document.getElementById("question-form");
const closeModal = document.querySelector(".close");
const cancelBtn = document.getElementById("cancel-btn");

// Initialize
document.addEventListener("DOMContentLoaded", () => {
  console.log("Questions Management Page initialized");
  checkServerConnection();
  setupEventListeners();
});

// Setup event listeners
function setupEventListeners() {
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
async function checkServerConnection() {
  try {
    const status = await HttpClient.checkServerStatus();
    if (status.status === "online") {
      updateServerStatus(true);
      await loadQuestions();
    } else {
      updateServerStatus(false);
    }
  } catch (error) {
    console.error("Server connection error:", error);
    updateServerStatus(false);
  }
}

// Update server status
function updateServerStatus(connected) {
  if (connected) {
    statusIndicator.className = "status-dot online";
    statusText.textContent = "Server Online";
    serverStatusText.textContent = "Online";
  } else {
    statusIndicator.className = "status-dot offline";
    statusText.textContent = "Server Offline";
    serverStatusText.textContent = "Offline";
  }
}

// Load questions from server
async function loadQuestions() {
  try {
    questions = await HttpClient.getQuestions();
    console.log("Loaded questions:", questions);
    displayQuestions();
    updateStats();
  } catch (error) {
    console.error("Failed to load questions:", error);
    questionsList.innerHTML = `
      <div class="error-state">
        <p>❌ Failed to load questions</p>
        <p class="hint">${error.message}</p>
      </div>
    `;
  }
}

// Display questions
function displayQuestions() {
  if (questions.length === 0) {
    questionsList.innerHTML = `
      <div class="empty-state">
        <p>📝 No questions available</p>
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
        <span class="question-number">Question #${q.id}</span>
        <div class="question-actions">
          <button class="btn-icon edit-btn" onclick="editQuestion(${q.id})" title="Edit">
            ✏️
          </button>
          <button class="btn-icon delete-btn" onclick="deleteQuestion(${q.id})" title="Delete">
            🗑️
          </button>
        </div>
      </div>
      <div class="question-content">
        <p class="question-text">${q.question}</p>
        <div class="options-grid">
          ${q.options
            .map(
              (opt, idx) => `
            <div class="option ${
              q.answer === String.fromCharCode(65 + idx) ? "correct-answer" : ""
            }">
              <span class="option-label">${String.fromCharCode(
                65 + idx
              )}.</span>
              <span class="option-text">${opt}</span>
              ${
                q.answer === String.fromCharCode(65 + idx)
                  ? '<span class="correct-badge">✓ Correct</span>'
                  : ""
              }
            </div>
          `
            )
            .join("")}
        </div>
      </div>
    </div>
  `
    )
    .join("");
}

// Update statistics
function updateStats() {
  totalQuestionsEl.textContent = questions.length;
}

// Open add question modal
function openAddQuestionModal() {
  editingQuestionId = null;
  modalTitle.textContent = "Add New Question";
  questionForm.reset();
  document.getElementById("question-id").value = "";
  modal.style.display = "flex";
}

// Open edit question modal
window.editQuestion = function (id) {
  const question = questions.find((q) => q.id === id);
  if (!question) return;

  editingQuestionId = id;
  modalTitle.textContent = "Edit Question";
  document.getElementById("question-id").value = id;
  document.getElementById("question-text").value = question.question;
  document.getElementById("option-a").value = question.options[0];
  document.getElementById("option-b").value = question.options[1];
  document.getElementById("option-c").value = question.options[2];
  document.getElementById("option-d").value = question.options[3];
  document.getElementById("correct-answer").value = question.answer;

  modal.style.display = "flex";
};

// Close modal
function closeQuestionModal() {
  modal.style.display = "none";
  questionForm.reset();
  editingQuestionId = null;
}

// Save question
async function saveQuestion(e) {
  e.preventDefault();

  const questionData = {
    question: document.getElementById("question-text").value.trim(),
    options: [
      document.getElementById("option-a").value.trim(),
      document.getElementById("option-b").value.trim(),
      document.getElementById("option-c").value.trim(),
      document.getElementById("option-d").value.trim(),
    ],
    answer: document.getElementById("correct-answer").value,
  };

  try {
    if (editingQuestionId) {
      // Update existing question
      const response = await HttpClient.updateQuestion(editingQuestionId, questionData);
      if (response.success) {
        alert("✅ Question updated successfully!");
        closeQuestionModal();
        await loadQuestions();
      } else {
        alert("❌ Failed to update question: " + response.message);
      }
    } else {
      // Add new question
      const response = await HttpClient.addQuestion(questionData);
      if (response.success) {
        alert("✅ Question added successfully!");
        closeQuestionModal();
        await loadQuestions();
      } else {
        alert("❌ Failed to add question: " + response.message);
      }
    }
  } catch (error) {
    console.error("Error saving question:", error);
    alert("❌ Error: " + error.message);
  }
}

// Delete question
window.deleteQuestion = async function (id) {
  if (!confirm("Are you sure you want to delete this question?")) {
    return;
  }

  try {
    const response = await HttpClient.deleteQuestion(id);
    if (response.success) {
      alert("✅ Question deleted successfully!");
      await loadQuestions();
    } else {
      alert("❌ Failed to delete question: " + response.message);
    }
  } catch (error) {
    console.error("Error deleting question:", error);
    alert("❌ Error: " + error.message);
  }
};

// Auto-refresh questions every 30 seconds
setInterval(() => {
  loadQuestions();
}, 30000);
