const API_BASE = '/api/songs';

// Load all songs on page load
document.addEventListener('DOMContentLoaded', () => {
    loadAllSongs();
    
    // Allow Enter key to trigger search
    document.getElementById('searchInput').addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            loadAllSongs();
        }
    });

    // Close modal when clicking outside of it
    document.getElementById('addFormOverlay').addEventListener('click', (e) => {
        if (e.target === document.getElementById('addFormOverlay')) {
            hideAddForm();
        }
    });

    // Update song count
    updateSongCount();
});

async function loadAllSongs() {
    const searchTerm = document.getElementById('searchInput').value.trim();
    
    showLoading();
    hideError();
    hideSuccess();
    
    try {
        let url = API_BASE;
        if (searchTerm) {
            url += `?search=${encodeURIComponent(searchTerm)}`;
        }
        
        const response = await fetch(url);
        if (!response.ok) {
            throw new Error('Failed to load songs');
        }
        const songs = await response.json();
        displaySongs(songs);
        updateSongCount();
    } catch (error) {
        showError('Failed to load songs: ' + error.message);
    } finally {
        hideLoading();
    }
}

function displaySongs(songs) {
    const songsList = document.getElementById('songsList');
    const emptyState = document.getElementById('emptyState');
    
    if (!songs || songs.length === 0) {
        songsList.classList.add('hidden');
        emptyState.classList.remove('hidden');
        return;
    }
    
    emptyState.classList.add('hidden');
    songsList.classList.remove('hidden');
    
    songsList.innerHTML = songs.map(song => `
        <div class="song-card">
            <div class="song-title">${escapeHtml(song.title)}</div>
            <div class="song-info">
                <strong>Artist:</strong> <span>${escapeHtml(song.artistName)}</span>
            </div>
            <div class="song-info">
                <strong>Album:</strong> <span>${escapeHtml(song.albumName)}</span>
            </div>
            <div class="song-duration">
                ⏱️ ${formatDuration(song.durationSeconds)}
            </div>
        </div>
    `).join('');
}

function formatDuration(seconds) {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    if (mins > 0) {
        return `${mins}m ${secs}s`;
    }
    return `${secs}s`;
}

function showAddForm() {
    document.getElementById('addForm').classList.remove('hidden');
    document.getElementById('addFormOverlay').classList.remove('hidden');
    setTimeout(() => {
        document.getElementById('songTitle').focus();
    }, 100);
}

function hideAddForm(event) {
    if (event && event.preventDefault) {
        event.preventDefault();
    }
    
    document.getElementById('addForm').classList.add('hidden');
    document.getElementById('addFormOverlay').classList.add('hidden');
    
    // Reset form
    const form = document.querySelector('.add-form');
    if (form) {
        form.reset();
    }
}

async function addSong(event) {
    event.preventDefault();
    
    const title = document.getElementById('songTitle').value.trim();
    const artistName = document.getElementById('artistName').value.trim();
    const albumName = document.getElementById('albumName').value.trim();
    const duration = document.getElementById('duration').value.trim();
    
    if (!title || !artistName || !albumName || !duration) {
        showError('Please fill in all fields');
        return;
    }
    
    const song = {
        title,
        artistName,
        albumName,
        durationSeconds: parseInt(duration)
    };
    
    if (isNaN(song.durationSeconds) || song.durationSeconds <= 0) {
        showError('Duration must be a positive number');
        return;
    }
    
    hideError();
    hideSuccess();
    
    try {
        const response = await fetch(API_BASE, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(song)
        });
        
        const result = await response.json();
        
        if (response.ok && result.success) {
            hideAddForm();
            showSuccess('✓ Song added successfully!');
            setTimeout(() => {
                loadAllSongs();
            }, 500);
        } else {
            showError(result.message || 'Failed to add song');
        }
    } catch (error) {
        showError('Failed to add song: ' + error.message);
    }
}

function showLoading() {
    document.getElementById('loading').classList.remove('hidden');
}

function hideLoading() {
    document.getElementById('loading').classList.add('hidden');
}

function showError(message) {
    const errorDiv = document.getElementById('error');
    errorDiv.textContent = message;
    errorDiv.classList.remove('hidden');
    errorDiv.classList.add('alert-error');
    
    // Auto hide after 5 seconds
    setTimeout(hideError, 5000);
}

function hideError() {
    document.getElementById('error').classList.add('hidden');
}

function showSuccess(message) {
    const successDiv = document.getElementById('success');
    successDiv.textContent = message;
    successDiv.classList.remove('hidden');
    successDiv.classList.add('alert-success');
}

function hideSuccess() {
    document.getElementById('success').classList.add('hidden');
}

function updateSongCount() {
    // This will be updated dynamically when songs are loaded
    const songCards = document.querySelectorAll('.song-card');
    const count = songCards.length;
    document.getElementById('songCount').textContent = `${count} Song${count !== 1 ? 's' : ''}`;
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// Keyboard shortcuts
document.addEventListener('keydown', (e) => {
    // ESC to close modal
    if (e.key === 'Escape') {
        hideAddForm();
    }
    // Ctrl+N or Cmd+N to open add form
    if ((e.ctrlKey || e.metaKey) && e.key === 'n') {
        e.preventDefault();
        showAddForm();
    }
});


