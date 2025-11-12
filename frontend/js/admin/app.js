// Admin Dashboard JavaScript - Member 1
import { HttpClient } from "../api/httpClient.js";

let serverConnected = false;
let connectedStudents = [];
let questions = [];
let pollingInterval;

// DOM Elements
const statusIndicator = document.getElementById("status-indicator");
const statusText = document.getElementById("status-text");
const studentCount = document.getElementById("student-count");
const questionCount = document.getElementById("question-count");
const studentsList = document.getElementById("students-list");
const activityLog = document.getElementById("activity-log");
const startQuizBtn = document.getElementById("start-quiz-btn");
const evaluateQuizBtn = document.getElementById("evaluate-quiz-btn");
const refreshBtn = document.getElementById("refresh-btn");
const startQuizCard = document.getElementById("start-quiz-card");

// Initialize
document.addEventListener("DOMContentLoaded", () => {
  addLog("System", "Admin Dashboard initialized");
  checkServerConnection();
  setupEventListeners();
});

// Setup event listeners
function setupEventListeners() {
  startQuizBtn.addEventListener("click", startQuiz);
  evaluateQuizBtn.addEventListener("click", evaluateQuiz);
  refreshBtn.addEventListener("click", refreshAll);
  
  // Add click handler for start quiz action card
  if (startQuizCard) {
    startQuizCard.addEventListener("click", startQuiz);
  }
}

// Check server connection
function checkServerConnection() {
  HttpClient.checkServerStatus()
    .then((data) => {
      serverConnected = true;
      updateServerStatus(true);
      addLog("Server", `Connected to Quiz Server on port ${data.port}`);
      startPolling();
      loadQuestionsCount();
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
  loadQuestionsCount();
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

// Load questions count from server
async function loadQuestionsCount() {
  try {
    questions = await HttpClient.getQuestions();
    questionCount.textContent = questions.length;
    addLog("System", `Loaded ${questions.length} questions`);
    
    // Update start button state
    startQuizBtn.disabled = connectedStudents.length === 0 || questions.length === 0;
  } catch (error) {
    console.error("Error loading questions:", error);
    questionCount.textContent = "?";
  }
}

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
      
      // Enable evaluate button after quiz starts
      evaluateQuizBtn.disabled = false;
    })
    .catch((error) => {
      console.error("Error starting quiz:", error);
      addLog("Error", "Failed to start quiz");
      alert("❌ Failed to start quiz. Check console for errors.");
    });
}

// ===== MEMBER 4 & 5: Evaluate Quiz and Distribute Results =====
async function evaluateQuiz() {
  if (!confirm("Evaluate all student answers and distribute results?\n\nThis will:\n1. Evaluate all submitted answers\n2. Calculate scores\n3. Send results to students\n4. Generate leaderboard")) {
    return;
  }
  
  evaluateQuizBtn.disabled = true;
  evaluateQuizBtn.textContent = "⏳ Evaluating...";
  
  try {
    const response = await fetch("http://localhost:8080/api/quiz/evaluate", {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      }
    });
    
    if (!response.ok) {
      throw new Error("Failed to evaluate quiz");
    }
    
    const result = await response.json();
    
    addLog("Admin", "✅ Quiz evaluated successfully!");
    addLog("System", "Results distributed to all students");
    
    alert("✅ Quiz Evaluation Complete!\n\n" +
          "• All answers have been evaluated\n" +
          "• Scores calculated with thread-safe synchronization\n" +
          "• Results distributed to students via NIO broadcast\n" +
          "• Leaderboard generated\n\n" +
          "Students can now view their results!");
    
    evaluateQuizBtn.textContent = "✅ Evaluated";
    
    // Redirect to leaderboard after 2 seconds
    setTimeout(() => {
      if (confirm("View the leaderboard now?")) {
        window.location.href = "leaderboard.html";
      } else {
        evaluateQuizBtn.disabled = false;
        evaluateQuizBtn.textContent = "📊 Evaluate & Show Results";
      }
    }, 2000);
    
  } catch (error) {
    console.error("Error evaluating quiz:", error);
    addLog("Error", "Failed to evaluate quiz: " + error.message);
    alert("❌ Failed to evaluate quiz!\n\nError: " + error.message);
    
    evaluateQuizBtn.disabled = false;
    evaluateQuizBtn.textContent = "📊 Evaluate & Show Results";
  }
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