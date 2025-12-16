package com.example.musiclibrary;

import com.example.musiclibrary.dao.SongDao;
import com.example.musiclibrary.db.DatabaseConnection;
import com.example.musiclibrary.model.Song;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class SongsServlet extends HttpServlet {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");

        try {
            Connection connection = DatabaseConnection.getConnection();
            SongDao songDao = new SongDao(connection);

            String pathInfo = request.getPathInfo();
            PrintWriter out = response.getWriter();

            if (pathInfo == null || pathInfo.equals("/")) {
                // Get all songs or search
                String search = request.getParameter("search");
                List<Song> songs;
                if (search != null && !search.trim().isEmpty()) {
                    songs = songDao.searchSongsByTitle(search);
                } else {
                    songs = songDao.getAllSongs();
                }
                out.print(objectMapper.writeValueAsString(songs));
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print("{\"error\":\"Not found\"}");
            }

            connection.close();
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            PrintWriter out = response.getWriter();
            out.print("{\"error\":\"" + e.getMessage() + "\"}");
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
            String pathInfo = request.getPathInfo();
            if (pathInfo == null || pathInfo.equals("/")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"success\": false, \"message\": \"Song ID required\"}");
                return;
            }

            // Extract song ID from path (e.g., /123)
            String idStr = pathInfo.substring(1);
            int songId;
            try {
                songId = Integer.parseInt(idStr);
            } catch (NumberFormatException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"success\": false, \"message\": \"Invalid song ID\"}");
                return;
            }

            Connection connection = DatabaseConnection.getConnection();
            SongDao songDao = new SongDao(connection);

            boolean success = songDao.deleteSong(songId);

            if (success) {
                response.setStatus(HttpServletResponse.SC_OK);
                out.print("{\"success\": true, \"message\": \"Song deleted successfully\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print("{\"success\": false, \"message\": \"Song not found\"}");
            }

            connection.close();
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\": false, \"message\": \"" + e.getMessage() + "\"}");
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
            SongDao songDao = new SongDao(connection);

            // Read JSON from request body
            StringBuilder jsonBody = new StringBuilder();
            String line;
            while ((line = request.getReader().readLine()) != null) {
                jsonBody.append(line);
            }

            Song song = objectMapper.readValue(jsonBody.toString(), Song.class);
            boolean success = songDao.addSong(song);

            if (success) {
                response.setStatus(HttpServletResponse.SC_OK);
                out.print("{\"success\": true, \"message\": \"Song added successfully\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.print("{\"success\": false, \"message\": \"Failed to add song\"}");
            }

            connection.close();
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\": false, \"message\": \"" + e.getMessage() + "\"}");
        }
    }
}

