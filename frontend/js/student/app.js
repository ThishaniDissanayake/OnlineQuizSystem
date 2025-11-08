// ============================================================
// COMPLETELY ISOLATED app.js - Each window is independent
// ============================================================

(function() {
  'use strict';
  
  // Generate TRULY unique window ID using multiple entropy sources
  const WINDOW_ID = 'W_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9) + '_' + performance.now();
  
  // Create completely isolated state object
  const STATE = {
    windowId: WINDOW_ID,
    studentName: "",
    socket: null,
    questions: [],
    studentAnswers: {},
    isConnected: false,
    hasSubmitted: false,
    isProcessingSubmission: false
  };
  
  console.log('🪟 [' + WINDOW_ID + '] NEW ISOLATED WINDOW CREATED');
  console.log('🔒 [' + WINDOW_ID + '] This window has its own STATE object');
  
  // DOM Elements
  let connectionSection, waitingSection, quizSection, connectionForm;
  let connectionIndicator, connectionText, questionsContainer;
  let currentQuestionSpan, totalQuestionsSpan, submitAnswersBtn;
  
  // Initialize
  document.addEventListener("DOMContentLoaded", function() {
    // Get DOM elements
    connectionSection = document.getElementById("connection-section");
    waitingSection = document.getElementById("waiting-section");
    quizSection = document.getElementById("quiz-section");
    connectionForm = document.getElementById("connection-form");
    connectionIndicator = document.getElementById("connection-indicator");
    connectionText = document.getElementById("connection-text");
    questionsContainer = document.getElementById("questions-container");
    currentQuestionSpan = document.getElementById("current-question");
    totalQuestionsSpan = document.getElementById("total-questions");
    submitAnswersBtn = document.getElementById("submit-answers-btn");
    
    setupEventListeners();
    console.log("📱 [" + WINDOW_ID + "] Student Portal Initialized");
  });
  
  function setupEventListeners() {
    connectionForm.addEventListener("submit", function(e) {
      e.preventDefault();
      connectToServer();
    });
    
    submitAnswersBtn.addEventListener("click", function() {
      submitAnswers();
    });
  }
  
  // ========== WEBSOCKET CONNECTION ==========
  function connectToServer() {
    const nameInput = document.getElementById("student-name");
    STATE.studentName = nameInput.value.trim();
    
    if (!STATE.studentName) {
      alert("Please enter your name!");
      return;
    }
    
    console.log('🔡 [' + WINDOW_ID + '] Connecting as: ' + STATE.studentName);
    
    try {
      STATE.socket = new WebSocket("ws://localhost:8081");
      
      STATE.socket.onopen = function() {
        console.log('✅ [' + WINDOW_ID + '] WebSocket OPENED for ' + STATE.studentName);
        STATE.isConnected = true;
        updateConnectionStatus(true);
        showWaitingScreen();
        
        STATE.socket.send(STATE.studentName);
        console.log('📤 [' + WINDOW_ID + '] Sent name: ' + STATE.studentName);
      };
      
      STATE.socket.onmessage = function(event) {
        handleWebSocketMessage(event.data);
      };
      
      STATE.socket.onerror = function(error) {
        console.error('❌ [' + WINDOW_ID + '] WebSocket error:', error);
        if (!STATE.hasSubmitted) {
          alert("Connection error!");
        }
        updateConnectionStatus(false);
      };
      
      STATE.socket.onclose = function(event) {
        console.log('🔌 [' + WINDOW_ID + '] WebSocket CLOSED');
        console.log('   Student: ' + STATE.studentName);
        console.log('   Code: ' + event.code);
        console.log('   Reason: ' + event.reason);
        console.log('   hasSubmitted: ' + STATE.hasSubmitted);
        console.log('   isProcessingSubmission: ' + STATE.isProcessingSubmission);
        
        STATE.isConnected = false;
        updateConnectionStatus(false);
        
        // Only alert if unexpected
        if (!STATE.hasSubmitted && !STATE.isProcessingSubmission && event.code !== 1000) {
          console.error('⚠️ [' + WINDOW_ID + '] UNEXPECTED CLOSURE!');
          alert("Connection lost!");
        }
      };
      
    } catch (error) {
      console.error('❌ [' + WINDOW_ID + '] Connection error:', error);
      alert("Failed to connect.");
    }
  }
  
  function handleWebSocketMessage(message) {
    console.log('📨 [' + WINDOW_ID + '] MESSAGE: ' + message.substring(0, 80));
    console.log('   My state: student=' + STATE.studentName + ', submitted=' + STATE.hasSubmitted);
    
    if (typeof message !== 'string') {
      console.error('❌ [' + WINDOW_ID + '] Non-string message!');
      return;
    }
    
    const trimmed = message.trim();
    
    if (trimmed.startsWith("QUESTION|")) {
      const q = parseQuestion(trimmed);
      if (q) {
        STATE.questions.push(q);
        displayQuestions();
      }
    }
    else if (trimmed === "START_QUIZ") {
      console.log('🎯 [' + WINDOW_ID + '] START_QUIZ for ' + STATE.studentName);
      showQuizScreen();
    }
    else if (trimmed === "SUBMISSION_ACK") {
      console.log('🔔 [' + WINDOW_ID + '] ================================');
      console.log('🔔 [' + WINDOW_ID + '] SUBMISSION_ACK RECEIVED');
      console.log('🔔 [' + WINDOW_ID + '] My student: ' + STATE.studentName);
      console.log('🔔 [' + WINDOW_ID + '] My hasSubmitted: ' + STATE.hasSubmitted);
      console.log('🔔 [' + WINDOW_ID + '] My isProcessingSubmission: ' + STATE.isProcessingSubmission);
      console.log('🔔 [' + WINDOW_ID + '] ================================');
      
      // CRITICAL: Check if THIS window submitted
      if (!STATE.hasSubmitted || !STATE.isProcessingSubmission) {
        console.error('🚫 [' + WINDOW_ID + '] ================================');
        console.error('🚫 [' + WINDOW_ID + '] REJECTING SUBMISSION_ACK!');
        console.error('🚫 [' + WINDOW_ID + '] This message is NOT for me!');
        console.error('🚫 [' + WINDOW_ID + '] I am: ' + STATE.studentName);
        console.error('🚫 [' + WINDOW_ID + '] I did NOT submit or not processing');
        console.error('🚫 [' + WINDOW_ID + '] ================================');
        return; // STOP HERE
      }
      
      console.log('✅ [' + WINDOW_ID + '] Valid ACK - I submitted, so processing...');
      handleSubmissionSuccess();
    }
    else if (trimmed.startsWith("Welcome")) {
      console.log('👋 [' + WINDOW_ID + '] ' + trimmed);
    }
    else {
      console.warn('⚠️ [' + WINDOW_ID + '] Unknown: ' + trimmed);
    }
  }
  
  function parseQuestion(msg) {
    const parts = msg.split("|");
    if (parts.length !== 4 || parts[0] !== "QUESTION") {
      return null;
    }
    return {
      id: parseInt(parts[1]),
      question: parts[2],
      options: parts[3].split(",")
    };
  }
  
  function displayQuestions() {
    if (STATE.questions.length === 0) {
      questionsContainer.innerHTML = '<p>No questions</p>';
      return;
    }
    
    totalQuestionsSpan.textContent = STATE.questions.length;
    
    questionsContainer.innerHTML = STATE.questions.map(function(q, idx) {
      return '<div class="question-card">' +
        '<h3>' + (idx + 1) + '. ' + escapeHtml(q.question) + '</h3>' +
        '<div class="options-list">' +
        q.options.map(function(opt, optIdx) {
          return '<label class="option-label">' +
            '<input type="radio" name="q' + q.id + '" value="' + escapeHtml(opt) + '" onchange="updateAnswerCount_' + WINDOW_ID.replace(/[^a-zA-Z0-9]/g, '_') + '()">' +
            '<span>' + String.fromCharCode(65 + optIdx) + '. ' + escapeHtml(opt) + '</span>' +
            '</label>';
        }).join('') +
        '</div></div>';
    }).join('');
  }
  
  function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }
  
  // Create unique global function for this window
  const updateFuncName = 'updateAnswerCount_' + WINDOW_ID.replace(/[^a-zA-Z0-9]/g, '_');
  window[updateFuncName] = function() {
    const answered = STATE.questions.filter(function(q) {
      return document.querySelector('input[name="q' + q.id + '"]:checked') !== null;
    }).length;
    
    currentQuestionSpan.textContent = answered;
    submitAnswersBtn.disabled = (answered !== STATE.questions.length);
  };
  
  function submitAnswers() {
    if (STATE.hasSubmitted) {
      console.warn('⚠️ [' + WINDOW_ID + '] Already submitted!');
      return;
    }
    
    STATE.studentAnswers = {};
    let allAnswered = true;
    
    STATE.questions.forEach(function(q) {
      const sel = document.querySelector('input[name="q' + q.id + '"]:checked');
      if (sel) {
        STATE.studentAnswers[q.id] = sel.value;
      } else {
        allAnswered = false;
      }
    });
    
    if (!allAnswered) {
      alert("Answer all questions!");
      return;
    }
    
    console.log('📤 [' + WINDOW_ID + '] ================================');
    console.log('📤 [' + WINDOW_ID + '] SUBMITTING QUIZ');
    console.log('📤 [' + WINDOW_ID + '] Student: ' + STATE.studentName);
    console.log('📤 [' + WINDOW_ID + '] ================================');
    
    STATE.hasSubmitted = true;
    STATE.isProcessingSubmission = true;
    submitAnswersBtn.disabled = true;
    submitAnswersBtn.textContent = "Submitting...";
    
    try {
      STATE.questions.forEach(function(q) {
        const ans = STATE.studentAnswers[q.id];
        const msg = 'ANSWER|' + q.id + '|' + ans;
        console.log('📤 [' + WINDOW_ID + '] ' + msg);
        STATE.socket.send(msg);
      });
      
      console.log('📤 [' + WINDOW_ID + '] Sending SUBMIT_COMPLETE');
      STATE.socket.send("SUBMIT_COMPLETE");
      console.log('⏳ [' + WINDOW_ID + '] Waiting for ACK...');
      
    } catch (error) {
      console.error('❌ [' + WINDOW_ID + '] Submit error:', error);
      STATE.hasSubmitted = false;
      STATE.isProcessingSubmission = false;
      submitAnswersBtn.disabled = false;
      submitAnswersBtn.textContent = "Submit Answers";
      alert("Submit failed!");
    }
  }
  
  function handleSubmissionSuccess() {
    console.log('🎉 [' + WINDOW_ID + '] ================================');
    console.log('🎉 [' + WINDOW_ID + '] SUCCESS - Processing completion');
    console.log('🎉 [' + WINDOW_ID + '] Student: ' + STATE.studentName);
    console.log('🎉 [' + WINDOW_ID + '] ================================');
    
    const key = 'quiz_' + WINDOW_ID;
    const data = {
      windowId: WINDOW_ID,
      studentName: STATE.studentName,
      totalQuestions: STATE.questions.length,
      totalAnswered: Object.keys(STATE.studentAnswers).length,
      timestamp: Date.now()
    };
    
    sessionStorage.setItem(key, JSON.stringify(data));
    console.log('💾 [' + WINDOW_ID + '] Saved to: ' + key);
    
    if (STATE.socket && STATE.socket.readyState === WebSocket.OPEN) {
      console.log('🔌 [' + WINDOW_ID + '] Closing socket');
      STATE.socket.close(1000, "Done");
      STATE.socket = null;
    }
    
    setTimeout(function() {
      console.log('🚀 [' + WINDOW_ID + '] Redirecting...');
      window.location.href = 'submission.html?w=' + WINDOW_ID;
    }, 150);
  }
  
  function showWaitingScreen() {
    connectionSection.style.display = "none";
    waitingSection.style.display = "block";
    document.querySelector(".student-name-display").textContent = "Connected as: " + STATE.studentName;
  }
  
  function showQuizScreen() {
    waitingSection.style.display = "none";
    quizSection.style.display = "block";
  }
  
  function updateConnectionStatus(connected) {
    if (connected) {
      connectionIndicator.classList.remove("offline");
      connectionIndicator.classList.add("online");
      connectionText.textContent = "Connected";
    } else {
      connectionIndicator.classList.remove("online");
      connectionIndicator.classList.add("offline");
      connectionText.textContent = "Not Connected";
    }
  }
  
  console.log('✅ [' + WINDOW_ID + '] Isolated instance ready!');
  
})();