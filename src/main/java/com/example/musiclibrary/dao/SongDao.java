package com.example.musiclibrary.dao;

import com.example.musiclibrary.model.Song;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SongDao {

    private final Connection connection;

    public SongDao(Connection connection) {
        this.connection = connection;
        ensureAudioFilePathColumn();
    }

    private void ensureAudioFilePathColumn() {
        try {
            String checkSql = "SELECT COUNT(*) AS cnt FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'songs' AND COLUMN_NAME = 'audio_file_path'";
            try (PreparedStatement stmt = connection.prepareStatement(checkSql);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int cnt = rs.getInt("cnt");
                    if (cnt == 0) {
                        String alter = "ALTER TABLE songs ADD COLUMN audio_file_path VARCHAR(500)";
                        try (PreparedStatement alterStmt = connection.prepareStatement(alter)) {
                            alterStmt.executeUpdate();
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Could not ensure audio_file_path column: " + e.getMessage());
        }
    }

    public List<Song> getAllSongs() {
        List<Song> songs = new ArrayList<>();
        String sql = "SELECT s.id, s.title, a.name AS artist_name, al.name AS album_name, s.duration_seconds, s.audio_file_path " +
                "FROM songs s " +
                "JOIN artists a ON s.artist_id = a.id " +
                "JOIN albums al ON s.album_id = al.id " +
                "ORDER BY s.title";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                songs.add(mapRowToSong(rs));
            }
        } catch (SQLException e) {
            System.out.println("Error while fetching songs: " + e.getMessage());
        }
        return songs;
    }

    public List<Song> searchSongsByTitle(String titlePart) {
        List<Song> songs = new ArrayList<>();
        String sql = "SELECT s.id, s.title, a.name AS artist_name, al.name AS album_name, s.duration_seconds, s.audio_file_path " +
                "FROM songs s " +
                "JOIN artists a ON s.artist_id = a.id " +
                "JOIN albums al ON s.album_id = al.id " +
                "WHERE LOWER(s.title) LIKE ? " +
                "ORDER BY s.title";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, "%" + titlePart.toLowerCase() + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    songs.add(mapRowToSong(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Error while searching songs: " + e.getMessage());
        }
        return songs;
    }

    public boolean addSong(Song song) {
        // For simplicity, we create (or reuse) an artist and album by name.
        try {
            int artistId = getOrCreateArtist(song.getArtistName());
            int albumId = getOrCreateAlbum(song.getAlbumName(), artistId);

            String sql = "INSERT INTO songs (title, artist_id, album_id, duration_seconds) VALUES (?, ?, ?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, song.getTitle());
                stmt.setInt(2, artistId);
                stmt.setInt(3, albumId);
                stmt.setInt(4, song.getDurationSeconds());
                int rows = stmt.executeUpdate();
                return rows > 0;
            }
        } catch (SQLException e) {
            System.out.println("Error while adding song: " + e.getMessage());
            return false;
        }
    }

    public boolean addSongWithFile(Song song) {
        // Add song with audio file path
        try {
            int artistId = getOrCreateArtist(song.getArtistName());
            int albumId = getOrCreateAlbum(song.getAlbumName(), artistId);

            String sql = "INSERT INTO songs (title, artist_id, album_id, duration_seconds, audio_file_path) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, song.getTitle());
                stmt.setInt(2, artistId);
                stmt.setInt(3, albumId);
                stmt.setInt(4, song.getDurationSeconds());
                stmt.setString(5, song.getAudioFilePath());
                int rows = stmt.executeUpdate();
                return rows > 0;
            }
        } catch (SQLException e) {
            System.out.println("Error while adding song with file: " + e.getMessage());
            return false;
        }
    }

    public Song getSongById(int songId) {
        String sql = "SELECT s.id, s.title, a.name AS artist_name, al.name AS album_name, s.duration_seconds, s.audio_file_path " +
                "FROM songs s " +
                "JOIN artists a ON s.artist_id = a.id " +
                "JOIN albums al ON s.album_id = al.id " +
                "WHERE s.id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, songId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToSong(rs);
                }
            }
        } catch (SQLException e) {
            System.out.println("Error while fetching song: " + e.getMessage());
        }
        return null;
    }

    public boolean deleteSong(int songId) {
        String sql = "DELETE FROM songs WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, songId);
            int rows = stmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.out.println("Error while deleting song: " + e.getMessage());
            return false;
        }
    }

    private int getOrCreateArtist(String name) throws SQLException {
        String selectSql = "SELECT id FROM artists WHERE name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(selectSql)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }

        String insertSql = "INSERT INTO artists (name) VALUES (?)";
        try (PreparedStatement stmt = connection.prepareStatement(insertSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                } else {
                    throw new SQLException("Failed to get generated artist ID.");
                }
            }
        }
    }

    private int getOrCreateAlbum(String name, int artistId) throws SQLException {
        String selectSql = "SELECT id FROM albums WHERE name = ? AND artist_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(selectSql)) {
            stmt.setString(1, name);
            stmt.setInt(2, artistId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }

        String insertSql = "INSERT INTO albums (name, artist_id) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(insertSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.setInt(2, artistId);
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                } else {
                    throw new SQLException("Failed to get generated album ID.");
                }
            }
        }
    }

    private Song mapRowToSong(ResultSet rs) throws SQLException {
        Song song = new Song();
        song.setId(rs.getInt("id"));
        song.setTitle(rs.getString("title"));
        song.setArtistName(rs.getString("artist_name"));
        song.setAlbumName(rs.getString("album_name"));
        song.setDurationSeconds(rs.getInt("duration_seconds"));
        song.setAudioFilePath(rs.getString("audio_file_path"));
        return song;
    }
}


