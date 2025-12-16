const API_BASE = '/api/songs';
const PLAYLIST_API = '/api/playlists';
let currentPlaylist = [];
let currentSongIndex = -1;
let currentPlaylistId = null;
let allSongs = [];

// Initialize on page load
document.addEventListener('DOMContentLoaded', () => {
    // Check authentication
    if (!localStorage.getItem('userId')) {
        window.location.href = '/login.html';
        return;
    }
    
    initializeEventListeners();
    loadAllSongs();
    loadAllPlaylists();
    updateUserInfo();
});

function updateUserInfo() {
    const username = localStorage.getItem('username');
    if (username) {
        // Add username to sidebar if needed
        const sidebar = document.querySelector('.sidebar-header');
        if (sidebar) {
            const userInfo = document.createElement('div');
            userInfo.style.cssText = 'color: #b3b3b3; font-size: 14px; margin-top: 10px;';
            userInfo.textContent = `👤 ${username}`;
            sidebar.appendChild(userInfo);
            
            // Add logout button
            const logoutBtn = document.createElement('button');
            logoutBtn.textContent = 'Logout';
            logoutBtn.className = 'btn-secondary';
            logoutBtn.style.cssText = 'width: 100%; margin-top: 20px;';
            logoutBtn.addEventListener('click', () => {
                localStorage.removeItem('userId');
                localStorage.removeItem('username');
                window.location.href = '/login.html';
            });
            sidebar.appendChild(logoutBtn);
        }
    }
}

// Event Listeners Setup
function initializeEventListeners() {
    // Navigation
    document.querySelectorAll('.nav-item').forEach(btn => {
        btn.addEventListener('click', () => {
            const section = btn.dataset.section;
            if (section) showSection(section);
        });
    });

    // Search
    document.getElementById('searchBtn').addEventListener('click', searchSongs);
    document.getElementById('searchInput').addEventListener('keypress', (e) => {
        if (e.key === 'Enter') searchSongs();
    });

    // Add Song
    const addSongBtn = document.getElementById('addSongBtn');
    if (addSongBtn) {
        addSongBtn.addEventListener('click', (e) => {
            e.preventDefault();
            showAddForm();
        });
    }
    const addSongForm = document.getElementById('addSongForm');
    if (addSongForm) {
        addSongForm.addEventListener('submit', addSong);
    }
    
    const closeAddForm = document.getElementById('closeAddForm');
    if (closeAddForm) {
        closeAddForm.addEventListener('click', hideAddForm);
    }
    
    const cancelAddForm = document.getElementById('cancelAddForm');
    if (cancelAddForm) {
        cancelAddForm.addEventListener('click', hideAddForm);
    }
    
    const addFormOverlay = document.getElementById('addFormOverlay');
    if (addFormOverlay) {
        addFormOverlay.addEventListener('click', (e) => {
            if (e.target === e.currentTarget) hideAddForm();
        });
    }

    // Create Playlist
    const createPlaylistBtn = document.getElementById('createPlaylistBtn');
    if (createPlaylistBtn) {
        createPlaylistBtn.addEventListener('click', (e) => {
            e.preventDefault();
            showCreatePlaylistForm();
        });
    }
    const createPlaylistFormElement = document.getElementById('createPlaylistFormElement');
    if (createPlaylistFormElement) {
        createPlaylistFormElement.addEventListener('submit', createPlaylist);
    }
    
    const closeCreatePlaylist = document.getElementById('closeCreatePlaylist');
    if (closeCreatePlaylist) {
        closeCreatePlaylist.addEventListener('click', hideCreatePlaylistForm);
    }
    
    const cancelCreatePlaylist = document.getElementById('cancelCreatePlaylist');
    if (cancelCreatePlaylist) {
        cancelCreatePlaylist.addEventListener('click', hideCreatePlaylistForm);
    }
    
    const createPlaylistOverlay = document.getElementById('createPlaylistOverlay');
    if (createPlaylistOverlay) {
        createPlaylistOverlay.addEventListener('click', (e) => {
            if (e.target === e.currentTarget) hideCreatePlaylistForm();
        });
    }

    // Add to Playlist
    const addToPlaylistBtn = document.getElementById('addToPlaylistBtn');
    if (addToPlaylistBtn) {
        addToPlaylistBtn.addEventListener('click', addSongToPlaylist);
    }
    
    const closeAddToPlaylist = document.getElementById('closeAddToPlaylist');
    if (closeAddToPlaylist) {
        closeAddToPlaylist.addEventListener('click', hideAddToPlaylistModal);
    }
    
    const cancelAddToPlaylist = document.getElementById('cancelAddToPlaylist');
    if (cancelAddToPlaylist) {
        cancelAddToPlaylist.addEventListener('click', hideAddToPlaylistModal);
    }
    
    const addToPlaylistOverlay = document.getElementById('addToPlaylistOverlay');
    if (addToPlaylistOverlay) {
        addToPlaylistOverlay.addEventListener('click', (e) => {
            if (e.target === e.currentTarget) hideAddToPlaylistModal();
        });
    }

    // Music Player
    document.getElementById('playPauseBtn').addEventListener('click', togglePlay);
    document.getElementById('prevBtn').addEventListener('click', previousSong);
    document.getElementById('nextBtn').addEventListener('click', nextSong);
    document.getElementById('progressBar').addEventListener('input', (e) => seekTo(e.target.value));
    
    const audioPlayer = document.getElementById('audioPlayer');
    audioPlayer.addEventListener('timeupdate', updateProgress);
    audioPlayer.addEventListener('ended', nextSong);
    audioPlayer.addEventListener('error', () => {
        showError('Error playing audio. Make sure audio file path is set in database.');
    });
}

// Navigation
function showSection(section) {
    document.querySelectorAll('.content-section').forEach(s => s.classList.add('hidden'));
    document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
    
    if (section === 'songs') {
        document.getElementById('songsSection').classList.remove('hidden');
        document.querySelectorAll('.nav-item')[0].classList.add('active');
    } else if (section === 'playlists') {
        document.getElementById('playlistsSection').classList.remove('hidden');
        document.querySelectorAll('.nav-item')[1].classList.add('active');
        loadAllPlaylists();
    }
}

// Songs
async function loadAllSongs() {
    showLoading();
    hideError();
    
    try {
        const response = await fetch(API_BASE);
        if (!response.ok) throw new Error('Failed to load songs');
        allSongs = await response.json();
        displaySongs(allSongs);
    } catch (error) {
        showError('Failed to load songs: ' + error.message);
    } finally {
        hideLoading();
    }
}

async function searchSongs() {
    const searchTerm = document.getElementById('searchInput').value.trim();
    if (!searchTerm) {
        loadAllSongs();
        return;
    }
    
    showLoading();
    hideError();
    
    try {
        const response = await fetch(`${API_BASE}?search=${encodeURIComponent(searchTerm)}`);
        if (!response.ok) throw new Error('Search failed');
        const songs = await response.json();
        displaySongs(songs);
    } catch (error) {
        showError('Search failed: ' + error.message);
    } finally {
        hideLoading();
    }
}

function displaySongs(songs) {
    const songsList = document.getElementById('songsList');
    
    if (!songs || songs.length === 0) {
        songsList.innerHTML = '<div class="empty-state"><div class="empty-icon">🎶</div><h3>No songs found</h3><p>Add your first song to get started!</p></div>';
        return;
    }
    
    songsList.innerHTML = songs.map((song, index) => `
        <div class="song-card" data-song-id="${song.id}" data-index="${index}">
            <div class="song-card-content">
                <div class="song-info-main">
                    <div class="song-title">${escapeHtml(song.title)}</div>
                    <div class="song-artist">${escapeHtml(song.artistName)}</div>
                    <div class="song-album">${escapeHtml(song.albumName)}</div>
                </div>
                <div class="song-actions">
                    <button class="btn-play" data-song-id="${song.id}" title="Play">▶</button>
                    <button class="btn-add-playlist" data-song-id="${song.id}" title="Add to Playlist">➕</button>
                </div>
            </div>
        </div>
    `).join('');
    
    // Attach event listeners to dynamically created buttons
    document.querySelectorAll('.btn-play').forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.stopPropagation();
            const songId = parseInt(btn.dataset.songId);
            playSong(songId, songs);
        });
    });
    
    document.querySelectorAll('.btn-add-playlist').forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.preventDefault();
            e.stopPropagation();
            const songId = parseInt(btn.dataset.songId);
            console.log('Add to playlist button clicked for song:', songId);
            if (songId) {
                showAddToPlaylistModal(songId);
            } else {
                console.error('Invalid song ID');
                alert('Error: Invalid song ID');
            }
        });
    });
    
    // Make entire card clickable
    document.querySelectorAll('.song-card').forEach(card => {
        card.addEventListener('click', (e) => {
            if (!e.target.closest('.song-actions')) {
                const songId = parseInt(card.dataset.songId);
                playSong(songId, songs);
            }
        });
    });
}

async function addSong(event) {
    event.preventDefault();
    
    const song = {
        title: document.getElementById('songTitle').value.trim(),
        artistName: document.getElementById('artistName').value.trim(),
        albumName: document.getElementById('albumName').value.trim(),
        durationSeconds: parseInt(document.getElementById('duration').value)
    };
    
    try {
        const response = await fetch(API_BASE, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(song)
        });
        
        const result = await response.json();
        if (response.ok && result.success) {
            hideAddForm();
            loadAllSongs();
            showSuccess('Song added successfully!');
        } else {
            showError(result.message || 'Failed to add song');
        }
    } catch (error) {
        showError('Failed to add song: ' + error.message);
    }
}

// Music Player
function playSong(songId, songsArray) {
    const songs = songsArray || allSongs;
    currentPlaylist = songs;
    currentSongIndex = songs.findIndex(s => s.id === songId);
    currentPlaylistId = null;
    
    if (currentSongIndex === -1) {
        showError('Song not found');
        return;
    }
    
    const song = songs[currentSongIndex];
    const audioPlayer = document.getElementById('audioPlayer');
    const player = document.getElementById('musicPlayer');
    
    // Try to play audio if file path exists
    if (song.audioFilePath) {
        audioPlayer.src = `/api/audio?id=${song.id}`;
        audioPlayer.load();
        audioPlayer.play().catch(err => {
            console.error('Play error:', err);
            showError('Could not play audio. File may not exist or format not supported.');
        });
    } else {
        // Show demo mode message
        audioPlayer.src = '';
        showSuccess(`Playing: ${song.title} (Demo mode - no audio file)`);
    }
    
    document.getElementById('nowPlayingTitle').textContent = song.title;
    document.getElementById('nowPlayingArtist').textContent = song.artistName;
    document.getElementById('totalTime').textContent = formatTime(song.durationSeconds);
    
    player.classList.remove('hidden');
    document.getElementById('playPauseBtn').textContent = '⏸';
}

function togglePlay() {
    const audioPlayer = document.getElementById('audioPlayer');
    const btn = document.getElementById('playPauseBtn');
    
    if (!audioPlayer.src) {
        showError('No song selected');
        return;
    }
    
    if (audioPlayer.paused) {
        audioPlayer.play().catch(err => {
            showError('Could not play audio');
        });
        btn.textContent = '⏸';
    } else {
        audioPlayer.pause();
        btn.textContent = '▶';
    }
}

function previousSong() {
    if (currentPlaylist.length === 0) return;
    currentSongIndex = (currentSongIndex - 1 + currentPlaylist.length) % currentPlaylist.length;
    playSong(currentPlaylist[currentSongIndex].id, currentPlaylist);
}

function nextSong() {
    if (currentPlaylist.length === 0) return;
    currentSongIndex = (currentSongIndex + 1) % currentPlaylist.length;
    playSong(currentPlaylist[currentSongIndex].id, currentPlaylist);
}

function seekTo(value) {
    const audioPlayer = document.getElementById('audioPlayer');
    if (audioPlayer.duration) {
        audioPlayer.currentTime = (value / 100) * audioPlayer.duration;
    }
}

function updateProgress() {
    const audioPlayer = document.getElementById('audioPlayer');
    const progressBar = document.getElementById('progressBar');
    const currentTime = document.getElementById('currentTime');
    
    if (audioPlayer.duration && !isNaN(audioPlayer.duration)) {
        const progress = (audioPlayer.currentTime / audioPlayer.duration) * 100;
        progressBar.value = progress;
        currentTime.textContent = formatTime(audioPlayer.currentTime);
    }
}

function formatTime(seconds) {
    if (!seconds || isNaN(seconds)) return '0:00';
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins}:${secs.toString().padStart(2, '0')}`;
}

// Playlists
async function loadAllPlaylists() {
    const userId = localStorage.getItem('userId');
    if (!userId) {
        console.error('User not logged in');
        return;
    }
    
    try {
        const response = await fetch(PLAYLIST_API, {
            headers: {
                'X-User-Id': userId
            }
        });
        if (!response.ok) throw new Error('Failed to load playlists');
        const playlists = await response.json();
        displayPlaylists(playlists);
    } catch (error) {
        console.error('Failed to load playlists:', error);
        const playlistsList = document.getElementById('playlistsList');
        if (playlistsList) {
            playlistsList.innerHTML = '<div class="empty-state">Failed to load playlists</div>';
        }
    }
}

function displayPlaylists(playlists) {
    const playlistsList = document.getElementById('playlistsList');
    
    if (!playlists || playlists.length === 0) {
        playlistsList.innerHTML = '<div class="empty-state"><div class="empty-icon">📋</div><h3>No playlists yet</h3><p>Create your first playlist!</p></div>';
        return;
    }
    
    playlistsList.innerHTML = playlists.map(playlist => `
        <div class="playlist-card" data-playlist-id="${playlist.id}">
            <div class="playlist-icon">📋</div>
            <div class="playlist-info">
                <h3>${escapeHtml(playlist.name)}</h3>
                <p class="playlist-count">${playlist.songCount || 0} songs</p>
                ${playlist.description ? `<p class="playlist-desc">${escapeHtml(playlist.description)}</p>` : ''}
            </div>
            <div class="playlist-actions">
                <button class="btn-sm btn-play-playlist" data-playlist-id="${playlist.id}" title="Play">▶</button>
                <button class="btn-sm btn-danger" data-playlist-id="${playlist.id}" title="Delete">🗑</button>
            </div>
        </div>
    `).join('');
    
    // Attach event listeners
    document.querySelectorAll('.playlist-card').forEach(card => {
        card.addEventListener('click', (e) => {
            if (!e.target.closest('.playlist-actions')) {
                const playlistId = parseInt(card.dataset.playlistId);
                viewPlaylist(playlistId);
            }
        });
    });
    
    document.querySelectorAll('.btn-play-playlist').forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.stopPropagation();
            const playlistId = parseInt(btn.dataset.playlistId);
            viewPlaylist(playlistId);
        });
    });
    
    document.querySelectorAll('.btn-danger').forEach(btn => {
        if (btn.classList.contains('btn-play-playlist')) return;
        btn.addEventListener('click', (e) => {
            e.stopPropagation();
            const playlistId = parseInt(btn.dataset.playlistId);
            deletePlaylist(playlistId);
        });
    });
}

async function viewPlaylist(playlistId) {
    const userId = localStorage.getItem('userId');
    if (!userId) {
        showError('You must be logged in');
        window.location.href = '/login.html';
        return;
    }
    
    try {
        const response = await fetch(`${PLAYLIST_API}/${playlistId}`, {
            headers: {
                'X-User-Id': userId
            }
        });
        if (!response.ok) throw new Error('Failed to load playlist');
        const playlist = await response.json();
        
        currentPlaylist = playlist.songs || [];
        currentPlaylistId = playlistId;
        
        if (currentPlaylist.length === 0) {
            document.getElementById('songsList').innerHTML = '<div class="empty-state">This playlist is empty</div>';
            showSection('songs');
            return;
        }
        
        // Display playlist songs
        displaySongs(currentPlaylist);
        showSection('songs');
        
        // Auto-play first song
        if (currentPlaylist.length > 0) {
            playSong(currentPlaylist[0].id, currentPlaylist);
        }
    } catch (error) {
        showError('Failed to load playlist: ' + error.message);
        console.error('View playlist error:', error);
    }
}

async function createPlaylist(event) {
    event.preventDefault();
    
    const userId = localStorage.getItem('userId');
    if (!userId) {
        showError('You must be logged in');
        window.location.href = '/login.html';
        return;
    }
    
    const playlist = {
        name: document.getElementById('playlistName').value.trim(),
        description: document.getElementById('playlistDescription').value.trim()
    };
    
    if (!playlist.name) {
        showError('Playlist name is required');
        return;
    }
    
    try {
        const response = await fetch(PLAYLIST_API, {
            method: 'POST',
            headers: { 
                'Content-Type': 'application/json',
                'X-User-Id': userId
            },
            body: JSON.stringify(playlist)
        });
        
        const contentType = response.headers.get('content-type');
        if (!contentType || !contentType.includes('application/json')) {
            const text = await response.text();
            showError('Server error: ' + text.substring(0, 100));
            return;
        }
        
        const result = await response.json();
        if (response.ok && result.success) {
            hideCreatePlaylistForm();
            loadAllPlaylists();
            showSuccess('Playlist created successfully!');
        } else {
            showError(result.message || 'Failed to create playlist');
        }
    } catch (error) {
        showError('Failed to create playlist: ' + error.message);
        console.error('Create playlist error:', error);
    }
}

async function addSongToPlaylist() {
    const playlistId = parseInt(document.getElementById('selectPlaylist').value);
    const songId = parseInt(document.getElementById('songToAddId').value);
    const userId = localStorage.getItem('userId');
    
    if (!playlistId || !songId) {
        showError('Please select a playlist');
        return;
    }
    
    if (!userId) {
        showError('You must be logged in');
        window.location.href = '/login.html';
        return;
    }
    
    try {
        const response = await fetch(`${PLAYLIST_API}/${playlistId}`, {
            method: 'POST',
            headers: { 
                'Content-Type': 'application/json',
                'X-User-Id': userId
            },
            body: JSON.stringify({ songId: songId })
        });
        
        // Check if response is JSON
        const contentType = response.headers.get('content-type');
        if (!contentType || !contentType.includes('application/json')) {
            const text = await response.text();
            showError('Server error: ' + text.substring(0, 100));
            console.error('Non-JSON response:', text);
            return;
        }
        
        const result = await response.json();
        if (response.ok && result.success) {
            hideAddToPlaylistModal();
            showSuccess('Song added to playlist!');
        } else {
            showError(result.message || 'Failed to add song');
        }
    } catch (error) {
        showError('Failed to add song: ' + error.message);
        console.error('Add to playlist error:', error);
    }
}

async function deletePlaylist(playlistId) {
    if (!confirm('Delete this playlist? This action cannot be undone.')) return;
    
    const userId = localStorage.getItem('userId');
    if (!userId) {
        showError('You must be logged in');
        return;
    }
    
    try {
        const response = await fetch(`${PLAYLIST_API}/${playlistId}`, {
            method: 'DELETE',
            headers: {
                'X-User-Id': userId
            }
        });
        
        const result = await response.json();
        if (response.ok && result.success) {
            loadAllPlaylists();
            showSuccess('Playlist deleted!');
        } else {
            showError(result.message || 'Failed to delete playlist');
        }
    } catch (error) {
        showError('Failed to delete playlist: ' + error.message);
    }
}

function showAddToPlaylistModal(songId) {
    console.log('showAddToPlaylistModal called with songId:', songId);
    
    if (!songId || isNaN(songId)) {
        alert('Error: Invalid song ID');
        return;
    }
    
    const songToAddId = document.getElementById('songToAddId');
    const overlay = document.getElementById('addToPlaylistOverlay');
    const modal = document.getElementById('addToPlaylistModal');
    
    if (!songToAddId) {
        console.error('songToAddId element not found!');
        alert('Error: Modal form not found. Please refresh the page.');
        return;
    }
    
    if (!overlay) {
        console.error('addToPlaylistOverlay element not found!');
        alert('Error: Modal overlay not found. Please refresh the page.');
        return;
    }
    
    if (!modal) {
        console.error('addToPlaylistModal element not found!');
        alert('Error: Modal not found. Please refresh the page.');
        return;
    }
    
    songToAddId.value = songId;
    console.log('Set songToAddId to:', songId);
    
    // Show modal first, then load playlists
    overlay.style.display = 'flex';
    overlay.classList.remove('hidden');
    modal.style.display = 'block';
    modal.classList.remove('hidden');
    console.log('Modal is now visible');
    
    // Load playlists
    loadPlaylistsForSelect().catch(err => {
        console.error('Error loading playlists:', err);
    });
}

function hideAddToPlaylistModal() {
    const overlay = document.getElementById('addToPlaylistOverlay');
    const modal = document.getElementById('addToPlaylistModal');
    if (overlay) {
        overlay.style.display = 'none';
        overlay.classList.add('hidden');
    }
    if (modal) {
        modal.style.display = 'none';
        modal.classList.add('hidden');
    }
}

async function loadPlaylistsForSelect() {
    const userId = localStorage.getItem('userId');
    if (!userId) {
        console.error('User not logged in');
        const select = document.getElementById('selectPlaylist');
        if (select) {
            select.innerHTML = '<option value="">Please log in first</option>';
        }
        return;
    }
    
    try {
        const response = await fetch(PLAYLIST_API, {
            headers: {
                'X-User-Id': userId
            }
        });
        
        if (!response.ok) {
            throw new Error('Failed to load playlists: ' + response.status);
        }
        
        const playlists = await response.json();
        const select = document.getElementById('selectPlaylist');
        
        if (!select) {
            console.error('selectPlaylist element not found!');
            return;
        }
        
        if (!playlists || playlists.length === 0) {
            select.innerHTML = '<option value="">No playlists yet. Create one in Playlists section!</option>';
            return;
        }
        
        select.innerHTML = '<option value="">Select a playlist...</option>' +
            playlists.map(p => `<option value="${p.id}">${escapeHtml(p.name)} (${p.songCount || 0} songs)</option>`).join('');
        console.log('Loaded', playlists.length, 'playlists');
    } catch (error) {
        console.error('Failed to load playlists:', error);
        const select = document.getElementById('selectPlaylist');
        if (select) {
            select.innerHTML = '<option value="">Error loading playlists. Try refreshing.</option>';
        }
    }
}

// Modals
function showAddForm() {
    console.log('showAddForm called');
    const overlay = document.getElementById('addFormOverlay');
    const form = document.getElementById('addForm');
    if (overlay && form) {
        overlay.classList.remove('hidden');
        form.classList.remove('hidden');
    } else {
        console.error('Add form elements not found');
    }
}

function hideAddForm() {
    const overlay = document.getElementById('addFormOverlay');
    const form = document.getElementById('addForm');
    const formElement = document.getElementById('addSongForm');
    if (overlay) overlay.classList.add('hidden');
    if (form) form.classList.add('hidden');
    if (formElement) formElement.reset();
}

function showCreatePlaylistForm() {
    console.log('showCreatePlaylistForm called');
    const overlay = document.getElementById('createPlaylistOverlay');
    const form = document.getElementById('createPlaylistForm');
    if (overlay && form) {
        overlay.classList.remove('hidden');
        form.classList.remove('hidden');
    } else {
        console.error('Create playlist form elements not found');
    }
}

function hideCreatePlaylistForm() {
    const overlay = document.getElementById('createPlaylistOverlay');
    const form = document.getElementById('createPlaylistForm');
    const formElement = document.getElementById('createPlaylistFormElement');
    if (overlay) overlay.classList.add('hidden');
    if (form) form.classList.add('hidden');
    if (formElement) formElement.reset();
}

// Utilities
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
    setTimeout(() => errorDiv.classList.add('hidden'), 5000);
}

function showSuccess(message) {
    const successDiv = document.getElementById('success');
    successDiv.textContent = message;
    successDiv.classList.remove('hidden');
    setTimeout(() => successDiv.classList.add('hidden'), 3000);
}

function hideError() {
    document.getElementById('error').classList.add('hidden');
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

