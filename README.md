# Online Quiz System

A multi-threaded client-server quiz application built with Java backend and JavaScript frontend.

## 🚀 Quick Start

### 1. Start Backend Server
```powershell
cd backend
java -cp "out;lib\gson-2.10.1.jar" server.QuizServer
```

### 2. Start Frontend Server
```powershell
cd frontend
npx --yes http-server -p 5500 -c-1 --cors
```

### 3. Access the Application

**Summary of all running services:**

- 🟢 **Backend Server**: http://localhost:8080 (HTTP API) & port 5000 (Socket)
- 🟢 **Frontend Server**: http://127.0.0.1:5500
- 🌐 **Main Page**: http://127.0.0.1:5500/index.html
- 🌐 **Admin Dashboard**: http://127.0.0.1:5500/pages/admin.html
- 🌐 **Question Panel**: http://127.0.0.1:5500/pages/question-panel.html

## 📋 Required Dependencies

- gson-2.10.1.jar (✅ Already included in `backend/lib/`)

If needed, download from:
https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar

Place it inside:
backend/lib/

## 🏗️ Project Structure

```
OnlineQuizSystem/
├── backend/
│   ├── src/
│   │   ├── server/
│   │   │   ├── QuizServer.java          (Main server - Member 1)
│   │   │   ├── ClientHandler.java       (Client connection handler)
│   │   │   ├── HttpServer.java          (HTTP API endpoints)
│   │   │   ├── questions/
│   │   │   │   ├── QuestionManager.java (Question loader - Member 2)
│   │   │   │   ├── Question.java
│   │   │   │   └── QuestionsHandler.java
│   │   │   ├── evaluation/
│   │   │   │   └── AnswerEvaluator.java
│   │   │   └── results/
│   │   │       └── ResultDistributor.java
│   │   ├── client/
│   │   │   └── QuizClient.java
│   │   └── utils/
│   │       └── Message.java
│   ├── resources/
│   │   └── questions.json               (Quiz questions)
│   └── lib/
│       └── gson-2.10.1.jar
├── frontend/
│   ├── index.html                       (Landing page)
│   ├── pages/
│   │   ├── admin.html                   (Admin Dashboard - Member 1)
│   │   └── question-panel.html          (Question Panel - Member 2)
│   ├── js/
│   │   ├── admin/
│   │   │   └── app.js                   (Admin logic)
│   │   ├── quiz/
│   │   │   ├── questionPanel.js
│   │   │   ├── loadQuestions.js
│   │   │   ├── submitAnswers.js
│   │   │   └── uiHelpers.js
│   │   └── api/
│   │       ├── endpoints.js
│   │       └── httpClient.js
│   └── styles/
└── README.md
```

## 👥 Team Members Implementation

### Member 1 – Server Setup & Client Connection Management
- ✅ ServerSocket initialization (port 5000)
- ✅ Multi-threaded client handling
- ✅ Connected client list management
- 🌐 **Frontend**: Admin Dashboard (`admin.html`)

### Member 2 – Question Broadcasting System
- ✅ Question storage and loading (`questions.json`)
- ✅ **Socket-based communication** using `ObjectOutputStream`
- ✅ Broadcast questions to all connected clients via Socket
- ✅ JSON serialization with Gson
- 🌐 **Frontend**: Question Panel (`question-panel.html`)
- 📡 **Networking**: ObjectOutputStream, ObjectInputStream, Socket Communication

## 🔧 Troubleshooting

### Server shows "Offline"
- Ensure backend server is running on port 8080
- Access frontend via HTTP (http://127.0.0.1:5500), not file://

### Port Already in Use
```powershell
# Find process using port
netstat -ano | findstr ":8080"
# Kill process (replace PID)
taskkill /F /PID <PID>
```
