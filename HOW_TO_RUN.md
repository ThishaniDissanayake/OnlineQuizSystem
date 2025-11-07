# How to Run the Online Quiz System

## ✅ Fixed Issues
1. **Added Gson Library** - Downloaded `gson-2.10.1.jar` and added to `backend/lib/`
2. **Fixed Frontend API Endpoint** - Corrected duplicate `/questions` in `loadQuestions.js`

## 🚀 Running the Application

### Step 1: Start the Backend Server

Open a PowerShell terminal and run:
```powershell
cd d:\L3S1\onlineQuize\OnlineQuizSystem\backend
java -cp "out;lib\gson-2.10.1.jar" server.QuizServer
```

You should see:
```
✅ Loaded 2 questions from JSON.
✅ HTTP API Server started on port 8080
✅ Quiz Server started on port 5000
```

### Step 2: Serve the Frontend

**IMPORTANT:** You cannot open HTML files directly (file:// protocol) because:
- JavaScript ES6 modules require a web server
- CORS prevents `file://` from accessing `http://localhost:8080`

**Option A: Use VS Code Live Server (Recommended)**
1. Right-click on `frontend/index.html` in VS Code
2. Select "Open with Live Server"
3. This will open `http://127.0.0.1:5500/index.html`

**Option B: Use Python HTTP Server**
```powershell
cd d:\L3S1\onlineQuize\OnlineQuizSystem\frontend
python -m http.server 3000
```
Then open: `http://localhost:3000`

**Option C: Use Node.js HTTP Server**
```powershell
cd d:\L3S1\onlineQuize\OnlineQuizSystem\frontend
npx http-server -p 3000
```
Then open: `http://localhost:3000`

### Step 3: Open the Application

Once Live Server is running:
- **Admin Dashboard**: `http://127.0.0.1:5500/pages/admin.html`
- **Quiz Panel (Student)**: `http://127.0.0.1:5500/pages/question-panel.html`

## 📝 API Endpoints

The backend provides these endpoints:
- `GET /api/status` - Server status
- `GET /api/questions` - Get all questions
- `GET /api/students` - Get connected students
- `POST /api/quiz/start` - Start quiz
- `POST /api/quiz/submit` - Submit answers

## 🧪 Testing the Backend

You can test if the backend is working using PowerShell:
```powershell
# Test server status
curl http://localhost:8080/api/status

# Test questions endpoint
curl http://localhost:8080/api/questions
```

## ⚠️ Common Issues

### Server shows "Offline" in Admin Dashboard
- Make sure the backend server is running on port 8080
- Check that you're accessing the frontend via HTTP (not file://)
- Verify CORS headers are being sent by the backend

### "Failed to load questions"
- Check browser console (F12) for errors
- Verify the backend `/api/questions` endpoint returns data
- Ensure you're using a web server (not opening HTML files directly)

### Port Already in Use
If you see "Address already in use", kill the existing process:
```powershell
# Find the process using port 8080
netstat -ano | findstr ":8080"

# Kill the process (replace PID with actual process ID)
taskkill /F /PID <PID>
```

## 📦 Files Modified
- `backend/.classpath` - Added Gson library
- `backend/lib/gson-2.10.1.jar` - Downloaded library
- `frontend/js/quiz/loadQuestions.js` - Fixed API endpoint URL
