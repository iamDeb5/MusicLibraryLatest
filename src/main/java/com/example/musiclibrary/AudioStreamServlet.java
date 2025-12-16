package com.example.musiclibrary;

import com.example.musiclibrary.dao.SongDao;
import com.example.musiclibrary.db.DatabaseConnection;
import com.example.musiclibrary.model.Song;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;

public class AudioStreamServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String songIdParam = request.getParameter("id");
        
        if (songIdParam == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().print("{\"error\":\"Song ID required\"}");
            return;
        }

        try {
            int songId = Integer.parseInt(songIdParam);
            Connection connection = DatabaseConnection.getConnection();
            SongDao songDao = new SongDao(connection);
            
            // Get song info
            Song song = songDao.getSongById(songId);
            if (song == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().print("{\"error\":\"Song not found\"}");
                connection.close();
                return;
            }

            String audioPath = song.getAudioFilePath();
            
            // If no audio file path, return placeholder or error
            if (audioPath == null || audioPath.trim().isEmpty()) {
                response.setContentType("application/json");
                response.getWriter().print("{\"error\":\"No audio file available for this song\"}");
                connection.close();
                return;
            }

            // Check if file exists
            Path filePath = Paths.get(audioPath);
            if (!Files.exists(filePath)) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().print("{\"error\":\"Audio file not found on server\"}");
                connection.close();
                return;
            }

            // Set content type for audio streaming
            String contentType = getContentType(audioPath);
            response.setContentType(contentType);
            response.setHeader("Accept-Ranges", "bytes");
            response.setHeader("Content-Disposition", "inline; filename=\"" + song.getTitle() + "\"");

            // Handle range requests for seeking
            String rangeHeader = request.getHeader("Range");
            long fileSize = Files.size(filePath);
            long start = 0;
            long end = fileSize - 1;

            if (rangeHeader != null) {
                String[] ranges = rangeHeader.replace("bytes=", "").split("-");
                start = Long.parseLong(ranges[0]);
                if (ranges.length > 1 && !ranges[1].isEmpty()) {
                    end = Long.parseLong(ranges[1]);
                }
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileSize);
            }

            response.setContentLengthLong(end - start + 1);

            // Stream the file
            try (InputStream fileStream = Files.newInputStream(filePath);
                 OutputStream out = response.getOutputStream()) {
                
                fileStream.skip(start);
                byte[] buffer = new byte[8192];
                long remaining = end - start + 1;
                
                while (remaining > 0) {
                    int read = fileStream.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                    if (read == -1) break;
                    out.write(buffer, 0, read);
                    remaining -= read;
                }
            }

            connection.close();
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().print("{\"error\":\"Invalid song ID\"}");
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().print("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private String getContentType(String filePath) {
        String lower = filePath.toLowerCase();
        if (lower.endsWith(".mp3")) return "audio/mpeg";
        if (lower.endsWith(".wav")) return "audio/wav";
        if (lower.endsWith(".ogg")) return "audio/ogg";
        if (lower.endsWith(".m4a")) return "audio/mp4";
        if (lower.endsWith(".flac")) return "audio/flac";
        return "audio/mpeg"; // default
    }
}

