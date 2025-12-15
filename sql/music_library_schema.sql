-- Create database (run as a user with permission to create databases)
CREATE DATABASE IF NOT EXISTS music_library;
USE music_library;

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
    CONSTRAINT fk_song_artist FOREIGN KEY (artist_id)
        REFERENCES artists (id) ON DELETE CASCADE,
    CONSTRAINT fk_song_album FOREIGN KEY (album_id)
        REFERENCES albums (id) ON DELETE CASCADE
);

-- Sample data
INSERT INTO artists (name) VALUES
    ('Arijit Singh'),
    ('Shreya Ghoshal'),
    ('Ed Sheeran')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Ensure we have IDs
SELECT * FROM artists;

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


