package com.example.musiclibrary;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StaticFileServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getPathInfo();
        if (path == null || path.equals("/")) {
            path = "/index.html";
        }

        // Remove leading slash
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        // Security: prevent directory traversal
        if (path.contains("..")) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // Try to load resource
        InputStream resourceStream = getClass().getClassLoader().getResourceAsStream("webapp/" + path);
        
        if (resourceStream == null) {
            // If not found, serve index.html for SPA routing
            resourceStream = getClass().getClassLoader().getResourceAsStream("webapp/index.html");
            if (resourceStream == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
        }

        // Set content type
        String contentType = getContentType(path);
        response.setContentType(contentType);
        response.setCharacterEncoding("UTF-8");

        // Copy stream to response
        try (OutputStream out = response.getOutputStream()) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = resourceStream.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        } finally {
            resourceStream.close();
        }
    }

    private String getContentType(String path) {
        if (path.endsWith(".html")) return "text/html";
        if (path.endsWith(".css")) return "text/css";
        if (path.endsWith(".js")) return "application/javascript";
        if (path.endsWith(".json")) return "application/json";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".svg")) return "image/svg+xml";
        return "text/plain";
    }
}

