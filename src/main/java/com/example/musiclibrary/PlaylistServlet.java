package com.example.musiclibrary;

import com.example.musiclibrary.dao.PlaylistDao;
import com.example.musiclibrary.db.DatabaseConnection;
import com.example.musiclibrary.model.Playlist;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class PlaylistServlet extends HttpServlet {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");

        try {
            // Get user ID from session or request header
            Integer userId = getUserId(request);
            if (userId == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().print("{\"error\":\"User not authenticated\"}");
                return;
            }

            Connection connection = DatabaseConnection.getConnection();
            PlaylistDao playlistDao = new PlaylistDao(connection);
            ensurePlaylistUserColumn(connection);

            String pathInfo = request.getPathInfo();
            PrintWriter out = response.getWriter();

            if (pathInfo == null || pathInfo.equals("/")) {
                // Get all playlists for this user
                List<Playlist> playlists = playlistDao.getAllPlaylists(userId);
                out.print(objectMapper.writeValueAsString(playlists));
            } else {
                // Get specific playlist with songs
                try {
                    int playlistId = Integer.parseInt(pathInfo.substring(1));
                    Playlist playlist = playlistDao.getPlaylistWithSongs(playlistId, userId);
                    if (playlist != null) {
                        out.print(objectMapper.writeValueAsString(playlist));
                    } else {
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        out.print("{\"error\":\"Playlist not found\"}");
                    }
                } catch (NumberFormatException e) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"error\":\"Invalid playlist ID\"}");
                }
            }

            connection.close();
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            PrintWriter out = response.getWriter();
            out.print("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
    
    private Integer getUserId(HttpServletRequest request) {
        // Try session first
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object userId = session.getAttribute("userId");
            if (userId != null) {
                return (Integer) userId;
            }
        }
        
        // Try from localStorage (sent as header)
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader != null) {
            try {
                return Integer.parseInt(userIdHeader);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        
        return null;
    }
    
    private void ensurePlaylistUserColumn(Connection connection) throws SQLException {
        // Check if user_id column exists, if not add it
        try {
            String checkSql = "SELECT COUNT(*) AS cnt FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'playlists' AND COLUMN_NAME = 'user_id'";
            try (PreparedStatement stmt = connection.prepareStatement(checkSql);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next() && rs.getInt("cnt") == 0) {
                    String alterSql = "ALTER TABLE playlists ADD COLUMN user_id INT";
                    try (PreparedStatement alterStmt = connection.prepareStatement(alterSql)) {
                        alterStmt.executeUpdate();
                    }
                    // Set existing playlists to user_id = 1 (or handle as needed)
                    String updateSql = "UPDATE playlists SET user_id = 1 WHERE user_id IS NULL";
                    try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                        updateStmt.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            // Column might already exist, ignore
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");

        PrintWriter out = response.getWriter();

        try {
            Connection connection = DatabaseConnection.getConnection();
            PlaylistDao playlistDao = new PlaylistDao(connection);

            String pathInfo = request.getPathInfo();

            // Get user ID
            Integer userId = getUserId(request);
            if (userId == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print("{\"success\": false, \"message\": \"User not authenticated\"}");
                connection.close();
                return;
            }
            
            ensurePlaylistUserColumn(connection);

            if (pathInfo == null || pathInfo.equals("/")) {
                // Create new playlist
                StringBuilder jsonBody = new StringBuilder();
                String line;
                while ((line = request.getReader().readLine()) != null) {
                    jsonBody.append(line);
                }

                PlaylistRequest playlistReq = objectMapper.readValue(jsonBody.toString(), PlaylistRequest.class);

                if (playlistReq.name == null || playlistReq.name.trim().isEmpty()) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"success\": false, \"message\": \"Playlist name is required\"}");
                    connection.close();
                    return;
                }

                boolean success = playlistDao.createPlaylist(playlistReq.name, playlistReq.description, userId);
                if (success) {
                    response.setStatus(HttpServletResponse.SC_OK);
                    out.print("{\"success\": true, \"message\": \"Playlist created successfully\"}");
                } else {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.print("{\"success\": false, \"message\": \"Failed to create playlist\"}");
                }
            } else {
                // Add song to playlist
                try {
                    int playlistId = Integer.parseInt(pathInfo.substring(1));

                    // Verify playlist belongs to user
                    Playlist playlist = playlistDao.getPlaylistById(playlistId, userId);
                    if (playlist == null) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        out.print("{\"success\": false, \"message\": \"Playlist not found or access denied\"}");
                        connection.close();
                        return;
                    }

                    StringBuilder jsonBody = new StringBuilder();
                    String line;
                    while ((line = request.getReader().readLine()) != null) {
                        jsonBody.append(line);
                    }

                    AddSongRequest addSongReq = objectMapper.readValue(jsonBody.toString(), AddSongRequest.class);

                    boolean success = playlistDao.addSongToPlaylist(playlistId, addSongReq.songId);
                    if (success) {
                        response.setStatus(HttpServletResponse.SC_OK);
                        out.print("{\"success\": true, \"message\": \"Song added to playlist\"}");
                    } else {
                        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        out.print("{\"success\": false, \"message\": \"Failed to add song\"}");
                    }
                } catch (NumberFormatException e) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"success\": false, \"message\": \"Invalid playlist ID\"}");
                }
            }

            connection.close();
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\": false, \"message\": \"" + e.getMessage() + "\"}");
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");

        PrintWriter out = response.getWriter();

        try {
            Integer userId = getUserId(request);
            if (userId == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print("{\"success\": false, \"message\": \"User not authenticated\"}");
                return;
            }

            String pathInfo = request.getPathInfo();
            if (pathInfo == null || pathInfo.equals("/")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"success\": false, \"message\": \"Playlist ID required\"}");
                return;
            }

            int playlistId = Integer.parseInt(pathInfo.substring(1));

            StringBuilder jsonBody = new StringBuilder();
            String line;
            while ((line = request.getReader().readLine()) != null) {
                jsonBody.append(line);
            }

            PlaylistRequest playlistReq = objectMapper.readValue(jsonBody.toString(), PlaylistRequest.class);

            Connection connection = DatabaseConnection.getConnection();
            PlaylistDao playlistDao = new PlaylistDao(connection);
            ensurePlaylistUserColumn(connection);

            // Verify playlist belongs to user
            Playlist playlist = playlistDao.getPlaylistById(playlistId, userId);
            if (playlist == null) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.print("{\"success\": false, \"message\": \"Playlist not found or access denied\"}");
                connection.close();
                return;
            }

            boolean success = playlistDao.updatePlaylist(playlistId, playlistReq.name, playlistReq.description);
            if (success) {
                response.setStatus(HttpServletResponse.SC_OK);
                out.print("{\"success\": true, \"message\": \"Playlist updated successfully\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.print("{\"success\": false, \"message\": \"Failed to update playlist\"}");
            }

            connection.close();
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\": false, \"message\": \"Invalid playlist ID\"}");
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\": false, \"message\": \"" + e.getMessage() + "\"}");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");

        PrintWriter out = response.getWriter();

        try {
            Integer userId = getUserId(request);
            if (userId == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print("{\"success\": false, \"message\": \"User not authenticated\"}");
                return;
            }

            String pathInfo = request.getPathInfo();
            if (pathInfo == null || pathInfo.equals("/")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"success\": false, \"message\": \"Playlist ID required\"}");
                return;
            }

            String[] parts = pathInfo.substring(1).split("/");
            int playlistId = Integer.parseInt(parts[0]);

            Connection connection = DatabaseConnection.getConnection();
            PlaylistDao playlistDao = new PlaylistDao(connection);
            ensurePlaylistUserColumn(connection);

            // Verify playlist belongs to user
            Playlist playlist = playlistDao.getPlaylistById(playlistId, userId);
            if (playlist == null) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.print("{\"success\": false, \"message\": \"Playlist not found or access denied\"}");
                connection.close();
                return;
            }

            if (parts.length == 2) {
                // Remove song from playlist
                try {
                    int songId = Integer.parseInt(parts[1]);
                    boolean success = playlistDao.removeSongFromPlaylist(playlistId, songId);
                    if (success) {
                        response.setStatus(HttpServletResponse.SC_OK);
                        out.print("{\"success\": true, \"message\": \"Song removed from playlist\"}");
                    } else {
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        out.print("{\"success\": false, \"message\": \"Song not found in playlist\"}");
                    }
                } catch (NumberFormatException e) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"success\": false, \"message\": \"Invalid song ID\"}");
                }
            } else {
                // Delete entire playlist
                boolean success = playlistDao.deletePlaylist(playlistId);
                if (success) {
                    response.setStatus(HttpServletResponse.SC_OK);
                    out.print("{\"success\": true, \"message\": \"Playlist deleted successfully\"}");
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print("{\"success\": false, \"message\": \"Playlist not found\"}");
                }
            }

            connection.close();
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\": false, \"message\": \"Invalid playlist ID\"}");
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\": false, \"message\": \"" + e.getMessage() + "\"}");
        }
    }

    // Helper classes for JSON deserialization
    public static class PlaylistRequest {
        public String name;
        public String description;
    }

    public static class AddSongRequest {
        public int songId;
    }
}
