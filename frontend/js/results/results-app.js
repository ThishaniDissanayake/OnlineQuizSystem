import { API_ENDPOINTS } from '../api/endpoints.js';
import { httpGet } from '../api/httpClient.js';

// Load results when page loads
window.addEventListener('DOMContentLoaded', () => {
    loadResults();
    // Auto-refresh every 10 seconds
    setInterval(loadResults, 10000);
});

// Make loadResults available globally
window.loadResults = loadResults;

async function loadResults() {
    const loadingSection = document.getElementById('loadingSection');
    const noResultsSection = document.getElementById('noResultsSection');
    const resultsTableSection = document.getElementById('resultsTableSection');
    const statsSection = document.getElementById('statsSection');
    
    try {
        loadingSection.style.display = 'block';
        noResultsSection.style.display = 'none';
        resultsTableSection.style.display = 'none';
        statsSection.style.display = 'none';

        // Fetch leaderboard and normalize to expected shape { totalQuestions, scores, timestamp }
        const leaderboard = await httpGet(API_ENDPOINTS.LEADERBOARD);
        
        if (!Array.isArray(leaderboard) || leaderboard.length === 0) {
            loadingSection.style.display = 'none';
            noResultsSection.style.display = 'block';
            return;
        }
        
        const totalQuestions = leaderboard[0]?.totalQuestions || 0;
        const scores = {};
        leaderboard.forEach(item => {
            // item: { studentName, score, totalQuestions, percentage, ... }
            if (item && item.studentName != null) {
                scores[item.studentName] = item.score;
            }
        });
        const response = { totalQuestions, scores, timestamp: Date.now() };
        
        if (!response || !response.scores || Object.keys(response.scores).length === 0) {
            loadingSection.style.display = 'none';
            noResultsSection.style.display = 'block';
            return;
        }

        // Process and display results
        displayResults(response);
        
        loadingSection.style.display = 'none';
        resultsTableSection.style.display = 'block';
        statsSection.style.display = 'grid';

    } catch (error) {
        console.error('Error loading results:', error);
        loadingSection.style.display = 'none';
        noResultsSection.style.display = 'block';
        
        const noResultsSection = document.getElementById('noResultsSection');
        noResultsSection.innerHTML = `
            <div class="no-results-icon">⚠️</div>
            <h3>Error Loading Results</h3>
            <p>${error.message || 'Failed to load results. Please try again.'}</p>
        `;
    }
}

function displayResults(data) {
    const { totalQuestions, scores, timestamp } = data;
    
    // Convert scores object to array and sort
    const resultsArray = Object.entries(scores).map(([name, score]) => ({
        name,
        score,
        percentage: totalQuestions > 0 ? (score / totalQuestions * 100) : 0
    }));
    
    // Sort by score (descending)
    resultsArray.sort((a, b) => b.score - a.score);
    
    // Update statistics
    updateStatistics(resultsArray, totalQuestions);
    
    // Display leaderboard
    displayLeaderboard(resultsArray, totalQuestions);
    
    // Update timestamp
    updateTimestamp(timestamp);
}

function updateStatistics(results, totalQuestions) {
    const totalStudents = results.length;
    const averagePercentage = results.length > 0
        ? results.reduce((sum, r) => sum + r.percentage, 0) / results.length
        : 0;
    const highestScore = results.length > 0 ? results[0].score : 0;
    
    document.getElementById('totalStudents').textContent = totalStudents;
    document.getElementById('totalQuestions').textContent = totalQuestions;
    document.getElementById('averageScore').textContent = averagePercentage.toFixed(1) + '%';
    document.getElementById('highestScore').textContent = `${highestScore}/${totalQuestions}`;
}

function displayLeaderboard(results, totalQuestions) {
    const tbody = document.getElementById('resultsTableBody');
    tbody.innerHTML = '';
    
    results.forEach((result, index) => {
        const rank = index + 1;
        const row = createResultRow(rank, result, totalQuestions);
        tbody.appendChild(row);
    });
}

function createResultRow(rank, result, totalQuestions) {
    const tr = document.createElement('tr');
    
    // Rank column with medal
    const rankTd = document.createElement('td');
    const rankClass = rank <= 3 ? `rank-${rank}` : '';
    const medal = rank === 1 ? '🥇' : rank === 2 ? '🥈' : rank === 3 ? '🥉' : '';
    rankTd.innerHTML = `<span class="rank ${rankClass}">${medal} ${rank}</span>`;
    tr.appendChild(rankTd);
    
    // Student name column
    const nameTd = document.createElement('td');
    nameTd.innerHTML = `<span class="student-name">${escapeHtml(result.name)}</span>`;
    tr.appendChild(nameTd);
    
    // Score column
    const scoreTd = document.createElement('td');
    const scoreClass = getScoreClass(result.percentage);
    scoreTd.innerHTML = `<span class="score-badge ${scoreClass}">${result.score}/${totalQuestions}</span>`;
    tr.appendChild(scoreTd);
    
    // Percentage column
    const percentageTd = document.createElement('td');
    percentageTd.innerHTML = `<strong>${result.percentage.toFixed(1)}%</strong>`;
    tr.appendChild(percentageTd);
    
    // Progress bar column
    const progressTd = document.createElement('td');
    progressTd.innerHTML = `
        <div class="percentage-bar">
            <div class="percentage-fill" style="width: ${result.percentage}%">
                ${result.percentage >= 20 ? result.percentage.toFixed(0) + '%' : ''}
            </div>
        </div>
    `;
    tr.appendChild(progressTd);
    
    return tr;
}

function getScoreClass(percentage) {
    if (percentage >= 80) return 'score-excellent';
    if (percentage >= 60) return 'score-good';
    return 'score-average';
}

function updateTimestamp(timestamp) {
    const timestampSection = document.getElementById('timestampSection');
    if (timestamp) {
        const date = new Date(timestamp);
        timestampSection.textContent = `Last updated: ${date.toLocaleString()}`;
    } else {
        timestampSection.textContent = `Last updated: ${new Date().toLocaleString()}`;
    }
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}
