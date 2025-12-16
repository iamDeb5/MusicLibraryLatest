package com.example.musiclibrary.db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {

    private static final String PROPERTIES_FILE = "/db.properties";

    public static Connection getConnection() throws SQLException {
        Properties properties = new Properties();

        try (InputStream input = DatabaseConnection.class.getResourceAsStream(PROPERTIES_FILE)) {
            if (input == null) {
                throw new SQLException("Could not find db.properties on classpath.");
            }
            properties.load(input);
        } catch (IOException e) {
            throw new SQLException("Failed to load db.properties: " + e.getMessage(), e);
        }

        String url = properties.getProperty("db.url");
        String user = properties.getProperty("db.user");
        String password = properties.getProperty("db.password");

        if (url == null || user == null) {
            throw new SQLException("db.url or db.user not set in db.properties");
        }

        Connection conn = DriverManager.getConnection(url, user, password);

        // Ensure playlists schema exists (safe to run on every startup)
        try (java.sql.Statement stmt = conn.createStatement()) {
            String createPlaylists = "CREATE TABLE IF NOT EXISTS playlists ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY,"
                    + "name VARCHAR(255) NOT NULL,"
                    + "description VARCHAR(500),"
                    + "user_id INT NOT NULL,"
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                    + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
                    + ")";
            stmt.execute(createPlaylists);

            // Try to create playlist_songs with foreign keys; if the songs table is missing,
            // fall back to creating playlist_songs without FK constraints.
            String createPlaylistSongsWithFK = "CREATE TABLE IF NOT EXISTS playlist_songs ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY,"
                    + "playlist_id INT NOT NULL,"
                    + "song_id INT NOT NULL,"
                    + "position INT DEFAULT 0,"
                    + "added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                    + "CONSTRAINT uc_playlist_song_unique UNIQUE (playlist_id, song_id),"
                    + "CONSTRAINT fk_playlist_song_playlist FOREIGN KEY (playlist_id) REFERENCES playlists(id) ON DELETE CASCADE,"
                    + "CONSTRAINT fk_playlist_song_song FOREIGN KEY (song_id) REFERENCES songs(id) ON DELETE CASCADE"
                    + ")";

            // Before creating playlist_songs with FKs, ensure the songs table exists.
            boolean songsExists = false;
            try (java.sql.ResultSet rs = conn.getMetaData().getTables(null, null, "songs", new String[]{"TABLE"})) {
                songsExists = rs.next();
            } catch (SQLException mdEx) {
                // ignore metadata errors, assume table may not exist
            }

            if (!songsExists) {
                // Create minimal artists, albums, and songs tables so playlist joins succeed
                try {
                    String createArtists = "CREATE TABLE IF NOT EXISTS artists ("
                            + "id INT AUTO_INCREMENT PRIMARY KEY,"
                            + "name VARCHAR(255) NOT NULL UNIQUE"
                            + ")";
                    stmt.execute(createArtists);

                    String createAlbums = "CREATE TABLE IF NOT EXISTS albums ("
                            + "id INT AUTO_INCREMENT PRIMARY KEY,"
                            + "name VARCHAR(255) NOT NULL,"
                            + "artist_id INT NOT NULL"
                            + ")";
                    stmt.execute(createAlbums);

                    String createSongs = "CREATE TABLE IF NOT EXISTS songs ("
                            + "id INT AUTO_INCREMENT PRIMARY KEY,"
                            + "title VARCHAR(255) NOT NULL,"
                            + "artist_id INT NOT NULL,"
                            + "album_id INT NOT NULL,"
                            + "duration_seconds INT NOT NULL,"
                            + "audio_file_path VARCHAR(500)"
                            + ")";
                    stmt.execute(createSongs);
                    System.out.println("Created minimal artists/albums/songs tables to support playlists.");
                } catch (SQLException createBaseEx) {
                    System.err.println("Failed to create base song-related tables: " + createBaseEx.getMessage());
                }
            }

            try {
                stmt.execute(createPlaylistSongsWithFK);
            } catch (SQLException fkEx) {
                // Possibly FK creation failed (e.g., different engine or ordering). Create without FK constraints.
                String createPlaylistSongsNoFK = "CREATE TABLE IF NOT EXISTS playlist_songs ("
                        + "id INT AUTO_INCREMENT PRIMARY KEY,"
                        + "playlist_id INT NOT NULL,"
                        + "song_id INT NOT NULL,"
                        + "position INT DEFAULT 0,"
                        + "added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                        + "CONSTRAINT uc_playlist_song_unique UNIQUE (playlist_id, song_id)"
                        + ")";
                try {
                    stmt.execute(createPlaylistSongsNoFK);
                } catch (SQLException innerEx) {
                    // If creation still fails, log to stderr but do not prevent connection
                    System.err.println("Failed to create playlist_songs table: " + innerEx.getMessage());
                }
            }
        } catch (SQLException e) {
            // Non-fatal: print and continue; application can still run without playlists support
            System.err.println("Warning: failed to ensure playlists schema: " + e.getMessage());
        }

        return conn;
    }
}


