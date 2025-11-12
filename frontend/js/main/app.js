// Main application entry point for landing page

document.addEventListener("DOMContentLoaded", () => {
  console.log("🎓 Online Quiz System - Landing Page initialized");
  console.log("📡 Backend Server: localhost:5000 (Socket) & localhost:8080 (HTTP)");

  // Add click handlers
  const adminLink = document.querySelector(".admin-card");
  const studentLink = document.querySelector(".student-card");

  if (adminLink) {
    adminLink.addEventListener("click", (e) => {
      console.log("→ Navigating to Admin Dashboard");
    });
  }

  if (studentLink) {
    studentLink.addEventListener("click", (e) => {
      console.log("→ Navigating to Student Portal");
    });
  }

  // Check if backend is accessible
  checkBackendStatus();
});

async function checkBackendStatus() {
  try {
    const response = await fetch("http://localhost:8080/api/status");
    const data = await response.json();
    console.log("✅ Backend Status:", data);
  } catch (error) {
    console.warn("⚠️ Backend not reachable. Make sure QuizServer.java is running.");
  }
}