-- Create database (run as a user with permission to create databases)
CREATE DATABASE IF NOT EXISTS music_library;
USE music_library;

-- Users table for authentication
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Artists table
CREATE TABLE IF NOT EXISTS artists (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

-- Albums table
CREATE TABLE IF NOT EXISTS albums (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    artist_id INT NOT NULL,
    CONSTRAINT fk_album_artist FOREIGN KEY (artist_id)
        REFERENCES artists (id) ON DELETE CASCADE,
    CONSTRAINT uc_album_artist_name UNIQUE (name, artist_id)
);

-- Songs table
CREATE TABLE IF NOT EXISTS songs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    artist_id INT NOT NULL,
    album_id INT NOT NULL,
    duration_seconds INT NOT NULL,
    audio_file_path VARCHAR(500),
    user_id INT,
    CONSTRAINT fk_song_artist FOREIGN KEY (artist_id)
        REFERENCES artists (id) ON DELETE CASCADE,
    CONSTRAINT fk_song_album FOREIGN KEY (album_id)
        REFERENCES albums (id) ON DELETE CASCADE,
    CONSTRAINT fk_song_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE SET NULL
);

-- Playlists table
CREATE TABLE IF NOT EXISTS playlists (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    user_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_playlist_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
);

-- Playlist songs table (many-to-many relationship)
CREATE TABLE IF NOT EXISTS playlist_songs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    playlist_id INT NOT NULL,
    song_id INT NOT NULL,
    position INT DEFAULT 0,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_playlist_song_playlist FOREIGN KEY (playlist_id)
        REFERENCES playlists (id) ON DELETE CASCADE,
    CONSTRAINT fk_playlist_song_song FOREIGN KEY (song_id)
        REFERENCES songs (id) ON DELETE CASCADE,
    CONSTRAINT uc_playlist_song_unique UNIQUE (playlist_id, song_id)
);

-- Sample data
INSERT INTO artists (name) VALUES
    ('Arijit Singh'),
    ('Shreya Ghoshal'),
    ('Ed Sheeran')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Insert albums (using artist IDs; adjust IDs if needed based on SELECT above)
INSERT INTO albums (name, artist_id) VALUES
    ('Romantic Hits', 1),
    ('Melodies', 2),
    ('Divide', 3)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Insert songs
INSERT INTO songs (title, artist_id, album_id, duration_seconds) VALUES
    ('Tum Hi Ho', 1, 1, 260),
    ('Channa Mereya', 1, 1, 280),
    ('Teri Meri', 2, 2, 250),
    ('Perfect', 3, 3, 263),
    ('Shape of You', 3, 3, 234)
ON DUPLICATE KEY UPDATE title = VALUES(title);

