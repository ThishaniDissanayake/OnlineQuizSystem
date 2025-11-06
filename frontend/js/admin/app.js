// Admin Dashboard JavaScript - Member 1
// Connects to Java QuizServer backend

import { HttpClient } from "../api/httpClient.js";

let serverConnected = false;
let connectedStudents = [];
let pollingInterval;

// DOM Elements
const statusIndicator = document.getElementById("status-indicator");
const statusText = document.getElementById("status-text");
const studentCount = document.getElementById("student-count");
const studentsList = document.getElementById("students-list");
const activityLog = document.getElementById("activity-log");
const startQuizBtn = document.getElementById("start-quiz-btn");
const refreshBtn = document.getElementById("refresh-btn");

// Initialize
document.addEventListener("DOMContentLoaded", () => {
  addLog("System", "Admin Dashboard initialized");
  checkServerConnection();
  setupEventListeners();
});

// Setup event listeners
function setupEventListeners() {
  startQuizBtn.addEventListener("click", startQuiz);
  refreshBtn.addEventListener("click", refreshStudentsList);
}

// Check server connection
function checkServerConnection() {
  HttpClient.checkServerStatus()
    .then((data) => {
      serverConnected = true;
      updateServerStatus(true);
      addLog("Server", `Connected to Quiz Server on port ${data.port}`);
      startPolling();
    })
    .catch((error) => {
      serverConnected = false;
      updateServerStatus(false);
      addLog(
        "Error",
        "Failed to connect to server. Make sure QuizServer is running."
      );
      // Retry after 5 seconds
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

// Start polling for connected students
function startPolling() {
  pollingInterval = setInterval(refreshStudentsList, 3000);
  refreshStudentsList();
}

// Refresh students list
function refreshStudentsList() {
  HttpClient.getConnectedStudents()
    .then((students) => {
      // Check for new students
      if (students.length > connectedStudents.length) {
        const newCount = students.length - connectedStudents.length;
        addLog("System", `${newCount} new student(s) connected`);
      }
      connectedStudents = students;
      updateStudentsList(students);
    })
    .catch((error) => {
      console.error("Error fetching students:", error);
      addLog("Error", "Failed to fetch student list");
    });
}

// Update students list UI
function updateStudentsList(students) {
  studentCount.textContent = students.length;

  // Enable/disable start quiz button
  startQuizBtn.disabled = students.length === 0;

  if (students.length === 0) {
    studentsList.innerHTML = `
            <div class="empty-state">
                <p>📭 No students connected yet</p>
                <p class="hint">Students will appear here when they connect to the server</p>
            </div>
        `;
    return;
  }

  studentsList.innerHTML = students
    .map(
      (student) => `
        <div class="student-item">
            <div class="student-info">
                <div class="student-avatar">${student.name.charAt(0)}</div>
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

// Start quiz
function startQuiz() {
  const studentCount = connectedStudents.length;
  if (studentCount === 0) {
    alert("No students connected!");
    return;
  }

  HttpClient.startQuiz()
    .then((data) => {
      addLog("Admin", `Quiz started with ${data.studentCount} students`);
      alert(`✅ Quiz started for ${data.studentCount} students!`);
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

  // Keep only last 50 logs
  if (activityLog.children.length > 50) {
    activityLog.removeChild(activityLog.lastChild);
  }
}

// Cleanup on page unload
window.addEventListener("beforeunload", () => {
  if (pollingInterval) {
    clearInterval(pollingInterval);
  }
});
