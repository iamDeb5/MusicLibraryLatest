package com.example.musiclibrary;

import com.example.musiclibrary.db.DatabaseConnection;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AuthServlet extends HttpServlet {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");

        String pathInfo = request.getPathInfo();
        PrintWriter out = response.getWriter();

        try {
            StringBuilder jsonBody = new StringBuilder();
            String line;
            while ((line = request.getReader().readLine()) != null) {
                jsonBody.append(line);
            }

            // Normalize pathInfo
            if (pathInfo == null) pathInfo = "";
            pathInfo = pathInfo.startsWith("/") ? pathInfo : "/" + pathInfo;
            
            if (pathInfo.equals("/register") || pathInfo.equals("/register/")) {
                registerUser(jsonBody.toString(), out, response);
            } else if (pathInfo.equals("/login") || pathInfo.equals("/login/")) {
                loginUser(jsonBody.toString(), out, request, response);
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print("{\"success\": false, \"message\": \"Invalid endpoint. Path: " + pathInfo + "\"}");
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            out.print("{\"success\": false, \"message\": \"" + escapeJson(e.getMessage()) + "\"}");
            e.printStackTrace();
        }
    }

    private void registerUser(String jsonBody, PrintWriter out, HttpServletResponse response) {
        try {
            AuthRequest authReq = objectMapper.readValue(jsonBody, AuthRequest.class);

            if (authReq.username == null || authReq.username.trim().isEmpty() ||
                authReq.email == null || authReq.email.trim().isEmpty() ||
                authReq.password == null || authReq.password.length() < 6) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"success\": false, \"message\": \"Invalid input. Password must be at least 6 characters.\"}");
                return;
            }

            Connection connection = null;
            try {
                connection = DatabaseConnection.getConnection();
                
                // Ensure users table exists
                ensureUsersTable(connection);
                
                // Check if username or email already exists
                String checkSql = "SELECT id FROM users WHERE username = ? OR email = ?";
                try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
                    checkStmt.setString(1, authReq.username);
                    checkStmt.setString(2, authReq.email);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next()) {
                            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                            out.print("{\"success\": false, \"message\": \"Username or email already exists\"}");
                            return;
                        }
                    }
                }

                // Hash password (simple SHA-256 for demo - use bcrypt in production)
                String hashedPassword = hashPassword(authReq.password);

                // Insert new user
                String insertSql = "INSERT INTO users (username, email, password) VALUES (?, ?, ?)";
                try (PreparedStatement stmt = connection.prepareStatement(insertSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                    stmt.setString(1, authReq.username);
                    stmt.setString(2, authReq.email);
                    stmt.setString(3, hashedPassword);
                    stmt.executeUpdate();

                    try (ResultSet rs = stmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            int userId = rs.getInt(1);
                            response.setStatus(HttpServletResponse.SC_CREATED);
                            out.print("{\"success\": true, \"message\": \"Account created successfully\", \"userId\": " + userId + ", \"username\": \"" + escapeJson(authReq.username) + "\"}");
                        } else {
                            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                            out.print("{\"success\": false, \"message\": \"Failed to create user\"}");
                        }
                    }
                }
            } catch (SQLException e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.print("{\"success\": false, \"message\": \"Database error: " + escapeJson(e.getMessage()) + "\"}");
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\": false, \"message\": \"" + escapeJson(e.getMessage()) + "\"}");
            e.printStackTrace();
        }
    }
    
    private void ensureUsersTable(Connection connection) throws SQLException {
        String createTableSql = "CREATE TABLE IF NOT EXISTS users (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "username VARCHAR(255) NOT NULL UNIQUE, " +
                "email VARCHAR(255) NOT NULL UNIQUE, " +
                "password VARCHAR(255) NOT NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";
        try (PreparedStatement stmt = connection.prepareStatement(createTableSql)) {
            stmt.executeUpdate();
        }
    }
    
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    private void loginUser(String jsonBody, PrintWriter out, HttpServletRequest request, HttpServletResponse response) {
        try {
            AuthRequest authReq = objectMapper.readValue(jsonBody, AuthRequest.class);

            if (authReq.username == null || authReq.password == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"success\": false, \"message\": \"Username and password required\"}");
                return;
            }

            Connection connection = null;
            try {
                connection = DatabaseConnection.getConnection();
                ensureUsersTable(connection);
                
                String hashedPassword = hashPassword(authReq.password);

                // Check credentials (username or email)
                String sql = "SELECT id, username FROM users WHERE (username = ? OR email = ?) AND password = ?";
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, authReq.username);
                    stmt.setString(2, authReq.username);
                    stmt.setString(3, hashedPassword);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            int userId = rs.getInt("id");
                            String username = rs.getString("username");

                            // Create session
                            HttpSession session = request.getSession(true);
                            session.setAttribute("userId", userId);
                            session.setAttribute("username", username);

                            response.setStatus(HttpServletResponse.SC_OK);
                            out.print("{\"success\": true, \"message\": \"Login successful\", \"userId\": " + userId + ", \"username\": \"" + escapeJson(username) + "\"}");
                        } else {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            out.print("{\"success\": false, \"message\": \"Invalid username or password\"}");
                        }
                    }
                }
            } catch (SQLException e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.print("{\"success\": false, \"message\": \"Database error: " + escapeJson(e.getMessage()) + "\"}");
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\": false, \"message\": \"" + escapeJson(e.getMessage()) + "\"}");
            e.printStackTrace();
        }
    }

    private String hashPassword(String password) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(password.getBytes());
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    // Helper class for JSON deserialization
    public static class AuthRequest {
        public String username;
        public String email;
        public String password;
    }
}

