package com.example.musiclibrary.dao;

import com.example.musiclibrary.model.Playlist;
import com.example.musiclibrary.model.Song;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PlaylistDao {
    private final Connection connection;

    public PlaylistDao(Connection connection) {
        this.connection = connection;
    }

    public List<Playlist> getAllPlaylists(int userId) {
        List<Playlist> playlists = new ArrayList<>();
        String sql = "SELECT p.id, p.name, p.description, p.created_at, p.updated_at, COUNT(ps.id) AS song_count " +
                "FROM playlists p " +
                "LEFT JOIN playlist_songs ps ON p.id = ps.playlist_id " +
                "WHERE p.user_id = ? " +
                "GROUP BY p.id " +
                "ORDER BY p.updated_at DESC";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    playlists.add(mapRowToPlaylist(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Error while fetching playlists: " + e.getMessage());
        }
        return playlists;
    }

    public Playlist getPlaylistById(int playlistId, int userId) {
        String sql = "SELECT p.id, p.name, p.description, p.created_at, p.updated_at, COUNT(ps.id) AS song_count " +
                "FROM playlists p " +
                "LEFT JOIN playlist_songs ps ON p.id = ps.playlist_id " +
                "WHERE p.id = ? AND p.user_id = ? " +
                "GROUP BY p.id";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, playlistId);
            stmt.setInt(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToPlaylist(rs);
                }
            }
        } catch (SQLException e) {
            System.out.println("Error while fetching playlist: " + e.getMessage());
        }
        return null;
    }

    public Playlist getPlaylistWithSongs(int playlistId, int userId) {
        Playlist playlist = getPlaylistById(playlistId, userId);
        if (playlist == null) return null;

        String sql = "SELECT s.id, s.title, a.name AS artist_name, al.name AS album_name, s.duration_seconds, s.audio_file_path " +
                "FROM songs s " +
                "JOIN artists a ON s.artist_id = a.id " +
                "JOIN albums al ON s.album_id = al.id " +
                "JOIN playlist_songs ps ON s.id = ps.song_id " +
                "WHERE ps.playlist_id = ? " +
                "ORDER BY ps.position, ps.added_at";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, playlistId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<Song> songs = new ArrayList<>();
                while (rs.next()) {
                    Song song = new Song();
                    song.setId(rs.getInt("id"));
                    song.setTitle(rs.getString("title"));
                    song.setArtistName(rs.getString("artist_name"));
                    song.setAlbumName(rs.getString("album_name"));
                    song.setDurationSeconds(rs.getInt("duration_seconds"));
                    song.setAudioFilePath(rs.getString("audio_file_path"));
                    songs.add(song);
                }
                playlist.setSongs(songs);
            }
        } catch (SQLException e) {
            System.out.println("Error while fetching playlist songs: " + e.getMessage());
        }
        return playlist;
    }

    public boolean createPlaylist(String name, String description, int userId) {
        String sql = "INSERT INTO playlists (name, description, user_id) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, description);
            stmt.setInt(3, userId);
            int rows = stmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.out.println("Error while creating playlist: " + e.getMessage());
            return false;
        }
    }

    public boolean updatePlaylist(int playlistId, String name, String description) {
        String sql = "UPDATE playlists SET name = ?, description = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, description);
            stmt.setInt(3, playlistId);
            int rows = stmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.out.println("Error while updating playlist: " + e.getMessage());
            return false;
        }
    }

    public boolean deletePlaylist(int playlistId) {
        String sql = "DELETE FROM playlists WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, playlistId);
            int rows = stmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.out.println("Error while deleting playlist: " + e.getMessage());
            return false;
        }
    }

    public boolean addSongToPlaylist(int playlistId, int songId) {
        // MySQL disallows modifying a table and selecting from the same table in a subquery
        // in the same statement in some cases. Compute next position first, then insert.
        String posSql = "SELECT COALESCE(MAX(position), 0) + 1 AS next_pos FROM playlist_songs WHERE playlist_id = ?";
        String insertSql = "INSERT INTO playlist_songs (playlist_id, song_id, position) VALUES (?, ?, ?)";
        try (PreparedStatement posStmt = connection.prepareStatement(posSql)) {
            posStmt.setInt(1, playlistId);
            try (ResultSet rs = posStmt.executeQuery()) {
                int nextPos = 1;
                if (rs.next()) {
                    nextPos = rs.getInt("next_pos");
                }
                try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                    insertStmt.setInt(1, playlistId);
                    insertStmt.setInt(2, songId);
                    insertStmt.setInt(3, nextPos);
                    int rows = insertStmt.executeUpdate();
                    return rows > 0;
                }
            }
        } catch (SQLException e) {
            System.out.println("Error while adding song to playlist: " + e.getMessage());
            return false;
        }
    }

    public boolean removeSongFromPlaylist(int playlistId, int songId) {
        String sql = "DELETE FROM playlist_songs WHERE playlist_id = ? AND song_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, playlistId);
            stmt.setInt(2, songId);
            int rows = stmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.out.println("Error while removing song from playlist: " + e.getMessage());
            return false;
        }
    }

    private Playlist mapRowToPlaylist(ResultSet rs) throws SQLException {
        Playlist playlist = new Playlist();
        playlist.setId(rs.getInt("id"));
        playlist.setName(rs.getString("name"));
        playlist.setDescription(rs.getString("description"));
        playlist.setCreatedAt(rs.getTimestamp("created_at"));
        playlist.setUpdatedAt(rs.getTimestamp("updated_at"));
        playlist.setSongCount(rs.getInt("song_count"));
        return playlist;
    }
}
