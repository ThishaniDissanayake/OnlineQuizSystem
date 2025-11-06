// Main application entry point for landing page

document.addEventListener("DOMContentLoaded", () => {
  console.log("Online Quiz System - Landing Page initialized");

  // Add any main page functionality here
  const adminLink = document.querySelector(".admin-card");
  const studentLink = document.querySelector(".student-card");

  if (adminLink) {
    adminLink.addEventListener("click", (e) => {
      console.log("Navigating to admin dashboard");
      // Navigation handled by href
    });
  }

  if (studentLink) {
    studentLink.addEventListener("click", (e) => {
      e.preventDefault();
      alert("Student portal coming soon!");
    });
  }
});
