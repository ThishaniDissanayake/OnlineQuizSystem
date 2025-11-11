import { API_ENDPOINTS } from '../api/endpoints.js';
import { httpGet } from '../api/httpClient.js';

let websocket = null;
let leaderboardData = [];
let isConnected = false;

window.addEventListener('DOMContentLoaded', () => {
    initializeLeaderboard();
});

window.refreshLeaderboard = refreshLeaderboard;
window.connectWebSocket = connectWebSocket;

function initializeLeaderboard() {
    loadInitialData();
    connectWebSocket();
    
    // Fallback: refresh data every 5 seconds if WebSocket fails
    setInterval(() => {
        if (!isConnected) {
            console.log('🔄 WebSocket not connected, refreshing via API');
            loadInitialData();
        }
    }, 5000);
}

async function loadInitialData() {
    try {
        if (leaderboardData.length === 0) {
            showLoading();
        }
        
        const response = await httpGet(API_ENDPOINTS.RESULTS);
        
        if (response && response.scores && Object.keys(response.scores).length > 0) {
            processLeaderboardData(response);
            displayLeaderboard();
            console.log('✅ Loaded', Object.keys(response.scores).length, 'results from API');
        } else {
            if (leaderboardData.length === 0) {
                showEmptyState();
            }
            console.log('ℹ️ No results available from API');
        }
    } catch (error) {
        console.error('❌ Error loading initial data:', error);
        if (leaderboardData.length === 0) {
            showEmptyState();
        }
    }
}

function connectWebSocket() {
    if (websocket && websocket.readyState === WebSocket.OPEN) {
        return;
    }

    console.log('Attempting WebSocket connection to ws://localhost:8081');
    updateConnectionStatus(false);

    try {
        websocket = new WebSocket('ws://localhost:8081');
        
        websocket.onopen = () => {
            console.log('✅ WebSocket connected successfully');
            isConnected = true;
            updateConnectionStatus(true);
            websocket.send('LeaderboardViewer');
            console.log('📤 Sent LeaderboardViewer identifier');
        };

        websocket.onmessage = (event) => {
            console.log('📨 Received:', event.data);
            handleWebSocketMessage(event.data);
        };

        websocket.onclose = (event) => {
            console.log('🔌 WebSocket disconnected. Code:', event.code, 'Reason:', event.reason);
            isConnected = false;
            updateConnectionStatus(false);
            setTimeout(() => {
                console.log('🔄 Attempting reconnection...');
                connectWebSocket();
            }, 3000);
        };

        websocket.onerror = (error) => {
            console.error('❌ WebSocket error:', error);
            isConnected = false;
            updateConnectionStatus(false);
        };

    } catch (error) {
        console.error('❌ Failed to create WebSocket:', error);
        updateConnectionStatus(false);
        setTimeout(connectWebSocket, 3000);
    }
}

function handleWebSocketMessage(message) {
    console.log('Received message:', message);
    
    if (message.startsWith('SCORE|')) {
        const scoreData = JSON.parse(message.substring(6));
        updateStudentScore(scoreData);
    } else if (message.startsWith('LEADERBOARD|')) {
        const leaderboardUpdate = JSON.parse(message.substring(12));
        updateFullLeaderboard(leaderboardUpdate);
    } else if (message.startsWith('STUDENT_UPDATE|')) {
        const studentsData = JSON.parse(message.substring(15));
        updateConnectedStudents(studentsData);
    }
}

function updateStudentScore(scoreData) {
    const { studentName, score, totalQuestions, percentage } = scoreData;
    
    const existingIndex = leaderboardData.findIndex(item => item.name === studentName);
    
    if (existingIndex >= 0) {
        const wasNew = leaderboardData[existingIndex].score !== score;
        leaderboardData[existingIndex] = {
            name: studentName,
            score: score,
            percentage: percentage,
            totalQuestions: totalQuestions,
            isNew: wasNew
        };
    } else {
        leaderboardData.push({
            name: studentName,
            score: score,
            percentage: percentage,
            totalQuestions: totalQuestions,
            isNew: true
        });
    }
    
    leaderboardData.sort((a, b) => b.score - a.score);
    displayLeaderboard();
    updateTimestamp();
}

function updateConnectedStudents(studentsData) {
    studentsData.forEach(student => {
        const existingIndex = leaderboardData.findIndex(item => item.name === student.name);
        
        if (existingIndex < 0) {
            leaderboardData.push({
                name: student.name,
                score: 0,
                percentage: 0,
                totalQuestions: 0,
                isNew: true,
                connected: true
            });
        } else {
            leaderboardData[existingIndex].connected = true;
        }
    });
    
    leaderboardData.sort((a, b) => b.score - a.score);
    displayLeaderboard();
    updateTimestamp();
}

function updateFullLeaderboard(leaderboardUpdate) {
    const { leaderboard } = leaderboardUpdate;
    
    leaderboardData = leaderboard.map(item => ({
        name: item.studentName,
        score: item.score,
        percentage: item.percentage,
        totalQuestions: item.totalQuestions,
        isNew: false
    }));
    
    displayLeaderboard();
    updateTimestamp();
}

function processLeaderboardData(response) {
    const { totalQuestions, scores } = response;
    
    leaderboardData = Object.entries(scores).map(([name, score]) => ({
        name,
        score,
        percentage: totalQuestions > 0 ? (score / totalQuestions * 100) : 0,
        totalQuestions,
        isNew: false
    }));
    
    leaderboardData.sort((a, b) => b.score - a.score);
}

function displayLeaderboard() {
    if (leaderboardData.length === 0) {
        showEmptyState();
        return;
    }

    showLeaderboard();
    
    const tbody = document.getElementById('leaderboardBody');
    tbody.innerHTML = '';
    
    leaderboardData.forEach((student, index) => {
        const rank = index + 1;
        const row = createLeaderboardRow(rank, student);
        tbody.appendChild(row);
    });
}

function createLeaderboardRow(rank, student) {
    const tr = document.createElement('tr');
    if (student.isNew) {
        tr.classList.add('new-entry');
        student.isNew = false;
    }
    
    const rankTd = document.createElement('td');
    if (student.score > 0) {
        const rankClass = rank <= 3 ? `rank-${rank}` : '';
        const medal = rank === 1 ? '🥇' : rank === 2 ? '🥈' : rank === 3 ? '🥉' : '';
        rankTd.innerHTML = `<span class="rank ${rankClass}">${medal} ${rank}</span>`;
    } else {
        rankTd.innerHTML = `<span style="color: #999;">-</span>`;
    }
    tr.appendChild(rankTd);
    
    const nameTd = document.createElement('td');
    const statusIcon = student.connected ? '🟢' : '🔴';
    const statusText = student.score === 0 ? ' (Connected)' : '';
    nameTd.innerHTML = `${statusIcon} <span class="student-name">${escapeHtml(student.name)}${statusText}</span>`;
    tr.appendChild(nameTd);
    
    const scoreTd = document.createElement('td');
    if (student.totalQuestions > 0) {
        scoreTd.innerHTML = `<span class="score">${student.score}/${student.totalQuestions}</span>`;
    } else {
        scoreTd.innerHTML = `<span style="color: #999;">Waiting...</span>`;
    }
    tr.appendChild(scoreTd);
    
    const percentageTd = document.createElement('td');
    if (student.totalQuestions > 0) {
        percentageTd.innerHTML = `<span class="percentage">${student.percentage.toFixed(1)}%</span>`;
    } else {
        percentageTd.innerHTML = `<span style="color: #999;">-</span>`;
    }
    tr.appendChild(percentageTd);
    
    const progressTd = document.createElement('td');
    if (student.totalQuestions > 0) {
        progressTd.innerHTML = `
            <div class="progress-bar">
                <div class="progress-fill" style="width: ${student.percentage}%"></div>
            </div>
        `;
    } else {
        progressTd.innerHTML = `<span style="color: #999;">Not started</span>`;
    }
    tr.appendChild(progressTd);
    
    return tr;
}

function showLoading() {
    document.getElementById('loadingSection').style.display = 'block';
    document.getElementById('emptySection').style.display = 'none';
    document.getElementById('leaderboardSection').style.display = 'none';
}

function showEmptyState() {
    document.getElementById('loadingSection').style.display = 'none';
    document.getElementById('emptySection').style.display = 'block';
    document.getElementById('leaderboardSection').style.display = 'none';
}

function showLeaderboard() {
    document.getElementById('loadingSection').style.display = 'none';
    document.getElementById('emptySection').style.display = 'none';
    document.getElementById('leaderboardSection').style.display = 'block';
}

function updateConnectionStatus(connected) {
    const indicator = document.getElementById('statusIndicator');
    const connectBtn = document.getElementById('connectBtn');
    
    if (connected) {
        indicator.textContent = '🟢 Live Updates';
        indicator.className = 'status-indicator status-live';
        connectBtn.disabled = true;
        connectBtn.textContent = '✅ Connected';
    } else {
        indicator.textContent = '🔴 Offline';
        indicator.className = 'status-indicator status-offline';
        connectBtn.disabled = false;
        connectBtn.textContent = '🔌 Connect';
    }
}

function updateTimestamp() {
    const timestampSection = document.getElementById('timestampSection');
    timestampSection.textContent = `Last updated: ${new Date().toLocaleString()}`;
}

async function refreshLeaderboard() {
    const refreshBtn = document.getElementById('refreshBtn');
    refreshBtn.disabled = true;
    refreshBtn.textContent = '🔄 Refreshing...';
    
    try {
        await loadInitialData();
    } finally {
        refreshBtn.disabled = false;
        refreshBtn.textContent = '🔄 Refresh';
    }
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}