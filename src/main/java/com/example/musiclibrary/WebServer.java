package com.example.musiclibrary;

import com.example.musiclibrary.db.DatabaseConnection;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.sql.Connection;
import java.sql.SQLException;

public class WebServer {
    private static final int PORT = 8080;

    public static void main(String[] args) {
        try {
            // Test database connection
            Connection connection = DatabaseConnection.getConnection();
            System.out.println("✓ Database connected successfully!");
            connection.close();

            // Create and configure Jetty server
            Server server = new Server(PORT);
            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context.setContextPath("/");
            server.setHandler(context);

            // Register servlets
            context.addServlet(new ServletHolder(new AuthServlet()), "/api/auth/*");
            context.addServlet(new ServletHolder(new SongsServlet()), "/api/songs/*");
            context.addServlet(new ServletHolder(new PlaylistServlet()), "/api/playlists/*");
            context.addServlet(new ServletHolder(new AudioStreamServlet()), "/api/audio/*");
            context.addServlet(new ServletHolder(new StaticFileServlet()), "/*");

            // Start server
            server.start();
            System.out.println("========================================");
            System.out.println("🎵 Online Music Library Web Server");
            System.out.println("========================================");
            System.out.println("Server running at: http://localhost:" + PORT);
            System.out.println("Open your browser and visit the URL above");
            System.out.println("Press Ctrl+C to stop the server");
            System.out.println("========================================");

            server.join();
        } catch (SQLException e) {
            System.err.println("❌ Database connection failed!");
            System.err.println("Error: " + e.getMessage());
            System.err.println("Please check your MySQL settings in src/main/resources/db.properties");
            System.exit(1);
        } catch (Exception e) {
            System.err.println("❌ Server startup failed!");
            e.printStackTrace();
            System.exit(1);
        }
    }
}

