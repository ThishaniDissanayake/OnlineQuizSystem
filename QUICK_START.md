# 🎯 Online Quiz System - Quick Start Guide

## 📋 Prerequisites

- **Java 17 or higher** (Check with: `java -version`)
- **Web Browser** (Chrome, Firefox, Edge, etc.)
- **PowerShell** (Windows)

## 🚀 How to Run

### **Option 1: Quick Start (Recommended)**

Open **TWO PowerShell terminals**:

#### **Terminal 1 - Start Server:**
```powershell
cd D:\L3S1\onlineQuize\OnlineQuizSystem
.\START_SERVER.ps1
```

This will:
- Compile all Java files
- Start the backend server
- Display server URLs

#### **Terminal 2 - Open Browser:**
```powershell
cd D:\L3S1\onlineQuize\OnlineQuizSystem
.\OPEN_BROWSER.ps1
```

This will:
- Check if server is running
- Open the quiz pages in your browser

---

### **Option 2: Manual Start**

#### **Terminal 1 - Compile & Run Server:**
```powershell
cd D:\L3S1\onlineQuize\OnlineQuizSystem\backend

# Compile
.\compile.ps1

# Run
.\run.ps1
```

#### **Terminal 2 - Open Browser Manually:**
- **Student Portal:** http://localhost:8080/pages/student.html
- **Admin Dashboard:** http://localhost:8080/pages/admin.html  
- **Leaderboard:** http://localhost:8080/pages/leaderboard.html

---

## 📖 How to Use

### **1. Start Quiz (Admin)**
1. Open Admin Dashboard: http://localhost:8080/pages/admin.html
2. Click **"View Questions"** to see available questions
3. Click **"Start Quiz"** to broadcast questions to all connected students
4. (Optional) Click **"Evaluate Quiz"** to manually trigger evaluation

### **2. Take Quiz (Student)**
1. Open Student Portal: http://localhost:8080/pages/student.html
2. Enter your name and click **"Attempt Quiz"**
3. Wait for the admin to start the quiz
4. Answer all questions
5. Click **"Submit Answers"**
6. You'll be redirected to the submission confirmation page

### **3. View Results**

#### **Automatic Evaluation:**
- Once **ALL connected students** submit their answers, the server automatically:
  - ✅ Evaluates all answers (Member 4 - Thread-safe scoring)
  - 📊 Calculates scores and rankings
  - 📤 Distributes results to all students (Member 5 - NIO broadcasting)
  - 🏆 Updates the leaderboard

#### **View Leaderboard:**
- Open: http://localhost:8080/pages/leaderboard.html
- Shows:
  - Top 3 students on podium
  - Complete rankings table
  - Statistics (total students, highest score, average score)
  - Auto-refreshes every 30 seconds

#### **View Individual Results:**
- After submission, click **"View Results"** button
- Or go to: http://localhost:8080/pages/result.html?student=YourName
- Shows:
  - Your total score and percentage
  - Correct/wrong answer count
  - Question-by-question review

---

## 🏗️ Architecture

### **Backend (Java - Port 8080, 8081, 5000)**
```
QuizServer.java          - Main server, coordinates all services
├── HttpServer.java      - REST API endpoints (port 8080)
├── WebSocketServer.java - Real-time WebSocket (port 8081)
├── ClientHandler.java   - TCP client handler (port 5000)
└── Components:
    ├── QuestionManager   - Manage questions (Member 2)
    ├── AnswerManager     - Record answers (Member 3)
    ├── AnswerEvaluator   - Thread-safe evaluation (Member 4)
    └── ResultDistributor - NIO result broadcasting (Member 5)
```

### **Frontend (HTML/JS/CSS)**
```
pages/
├── student.html      - Student portal (WebSocket connection)
├── admin.html        - Admin dashboard
├── submission.html   - Submission confirmation
├── result.html       - Individual student results
└── leaderboard.html  - Full leaderboard with podium
```

---

## 🔧 API Endpoints

### **Questions Management (Member 2)**
- `GET  /api/questions` - Get all questions
- `POST /api/questions/add` - Add new question
- `PUT  /api/questions/update?id=X` - Update question
- `DELETE /api/questions/delete?id=X` - Delete question

### **Quiz Control (Member 3)**
- `GET  /api/students` - Get connected students
- `POST /api/quiz/start` - Start quiz (broadcast questions)

### **Evaluation & Results (Member 4 & 5)**
- `POST /api/quiz/evaluate` - Manually trigger evaluation
- `GET  /api/results/leaderboard` - Get full leaderboard
- `GET  /api/results/student?name=X` - Get student result

### **System**
- `GET  /api/status` - Server health check

---

## 🧪 Testing the System

### **Test with Multiple Students:**

1. Open multiple browser tabs/windows
2. In each tab, open: http://localhost:8080/pages/student.html
3. Enter different names (e.g., "Alice", "Bob", "Charlie")
4. Connect all students
5. In admin dashboard, click **"Start Quiz"**
6. Have each student answer and submit
7. When the last student submits → **Automatic evaluation triggers!**
8. Open leaderboard to see results

### **Expected Terminal Output (Clean!):**

```
✅ Quiz Server (TCP) started on port 5000
✅ HTTP API Server started on port 8080
✅ WebSocket Server started on port 8081
👤 [WS-xxx] NEW STUDENT REGISTERED: Alice
👤 [WS-xxx] NEW STUDENT REGISTERED: Bob
📢 Broadcasting 5 questions to 2 WebSocket clients
✅ [WS-xxx] Alice SUBMITTED QUIZ
✅ [WS-xxx] Bob SUBMITTED QUIZ
🎯 All WebSocket clients have submitted. Triggering evaluation...
════════════════════════════════════════════════════════════
📊 MEMBER 4: STARTING ANSWER EVALUATION & SCORING
════════════════════════════════════════════════════════════
🧑 Evaluating Student #1: Alice
   ✅ Correct: 4/5
   📈 Score: 4.00/5
🧑 Evaluating Student #2: Bob
   ✅ Correct: 5/5
   📈 Score: 5.00/5
✅ EVALUATION COMPLETE - 2 students evaluated
════════════════════════════════════════════════════════════
📤 MEMBER 5: STARTING RESULT DISTRIBUTION
════════════════════════════════════════════════════════════
📨 Sending Individual Results:
   1. ✅ Sent to: Alice | Score: 4.00/5
   2. ✅ Sent to: Bob | Score: 5.00/5
📊 Broadcasting Leaderboard:
   1. Bob - 5.00 (100.00%)
   2. Alice - 4.00 (80.00%)
✅ RESULT DISTRIBUTION COMPLETE
════════════════════════════════════════════════════════════
```

---

## 📂 Project Structure

```
OnlineQuizSystem/
├── START_SERVER.ps1       # Quick start script
├── OPEN_BROWSER.ps1       # Browser launcher
├── backend/
│   ├── compile.ps1        # Compile script
│   ├── run.ps1            # Run script
│   ├── lib/
│   │   └── gson-2.10.1.jar
│   ├── resources/
│   │   ├── questions.json # Quiz questions
│   │   └── answers.json   # Student answers (auto-generated)
│   └── src/
│       ├── server/
│       │   ├── QuizServer.java
│       │   ├── HttpServer.java
│       │   ├── WebSocketServer.java
│       │   ├── ClientHandler.java
│       │   ├── evaluation/
│       │   │   └── AnswerEvaluator.java (Member 4)
│       │   ├── questions/
│       │   │   ├── QuestionManager.java
│       │   │   ├── AnswerManager.java
│       │   │   └── Question.java
│       │   └── results/
│       │       └── ResultDistributor.java (Member 5)
│       └── utils/
│           └── Message.java
└── frontend/
    ├── pages/
    │   ├── student.html
    │   ├── admin.html
    │   ├── submission.html
    │   ├── result.html
    │   └── leaderboard.html
    ├── js/
    └── styles/
```

---

## 🔍 Troubleshooting

### **"Error: Unable to initialize main class"**
- **Problem:** Class version mismatch or not compiled
- **Solution:** Run `.\compile.ps1` first to recompile with your Java version

### **"Port already in use"**
- **Problem:** Server already running
- **Solution:** Close existing server or use different port

### **"Connection refused" in browser**
- **Problem:** Server not running
- **Solution:** Start the server first with `.\START_SERVER.ps1`

### **Questions not loading**
- **Problem:** `resources/questions.json` missing or invalid
- **Solution:** Add questions via Admin Dashboard

### **Results not appearing**
- **Problem:** No evaluation triggered yet
- **Solution:** 
  - Wait for all students to submit (auto-trigger)
  - Or click "Evaluate Quiz" in admin dashboard

---

## 🎯 Member Contributions

### **Member 4: Answer Evaluation & Scoring**
- **File:** `backend/src/server/evaluation/AnswerEvaluator.java`
- **Features:**
  - Thread-safe score calculation using `synchronized` blocks
  - `ConcurrentHashMap` for concurrent score storage
  - Calculates total marks and stores detailed results
  - Question-by-question breakdown
- **Networking:** Synchronization, Thread Communication

### **Member 5: Result Distribution & Leaderboard**
- **File:** `backend/src/server/results/ResultDistributor.java`
- **Features:**
  - Sends individual scores to each student
  - Broadcasts final leaderboard to all clients
  - Non-blocking result broadcast using NIO concepts
  - Real-time WebSocket communication
- **Networking:** NIO, Non-blocking Channels, Broadcast Communication

---

## 📞 Support

- Check server terminal for error messages
- Use browser DevTools (F12) to see console logs
- Verify all files are compiled: `backend/out/server/*.class`

---

## ✅ Success Indicators

When everything works:
1. ✅ Server starts with 3 services (TCP, HTTP, WebSocket)
2. ✅ Students can connect and see "Connected" status
3. ✅ Admin can start quiz and questions appear
4. ✅ Students can submit answers
5. ✅ Evaluation runs automatically after all submissions
6. ✅ Leaderboard shows sorted results with podium
7. ✅ Individual results show question-by-question breakdown

**Enjoy your quiz system! 🎓🚀**
