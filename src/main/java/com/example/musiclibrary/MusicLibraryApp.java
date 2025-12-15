package com.example.musiclibrary;

import com.example.musiclibrary.dao.SongDao;
import com.example.musiclibrary.db.DatabaseConnection;
import com.example.musiclibrary.model.Song;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

public class MusicLibraryApp {

    private final SongDao songDao;
    private final Scanner scanner;

    public MusicLibraryApp(Connection connection) {
        this.songDao = new SongDao(connection);
        this.scanner = new Scanner(System.in);
    }

    public static void main(String[] args) {
        System.out.println("=== Online Music Library ===");
        try {
            Connection connection = DatabaseConnection.getConnection();
            MusicLibraryApp app = new MusicLibraryApp(connection);
            app.run();
        } catch (SQLException e) {
            System.out.println("Could not connect to database. Please check your MySQL settings in db.properties.");
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void run() {
        int choice;
        do {
            printMenu();
            choice = readInt("Enter your choice: ");
            switch (choice) {
                case 1:
                    listAllSongs();
                    break;
                case 2:
                    searchSongsByTitle();
                    break;
                case 3:
                    addNewSong();
                    break;
                case 0:
                    System.out.println("Exiting. Goodbye!");
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        } while (choice != 0);
    }

    private void printMenu() {
        System.out.println();
        System.out.println("1. List all songs");
        System.out.println("2. Search songs by title");
        System.out.println("3. Add new song");
        System.out.println("0. Exit");
    }

    private int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                if (!scanner.hasNextLine()) {
                    System.out.println("\nNo input available. Exiting...");
                    return 0;
                }
                String line = scanner.nextLine();
                if (line == null || line.trim().isEmpty()) {
                    System.out.println("Please enter a valid number.");
                    continue;
                }
                return Integer.parseInt(line.trim());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            } catch (Exception e) {
                System.out.println("Error reading input: " + e.getMessage());
                return 0;
            }
        }
    }

    private void listAllSongs() {
        List<Song> songs = songDao.getAllSongs();
        if (songs.isEmpty()) {
            System.out.println("No songs found.");
            return;
        }
        System.out.println("--- All Songs ---");
        for (Song song : songs) {
            System.out.println(song);
        }
    }

    private void searchSongsByTitle() {
        System.out.print("Enter part of the song title to search: ");
        try {
            if (!scanner.hasNextLine()) {
                System.out.println("No input available.");
                return;
            }
            String query = scanner.nextLine();
            if (query == null || query.trim().isEmpty()) {
                System.out.println("Search query cannot be empty.");
                return;
            }
            List<Song> songs = songDao.searchSongsByTitle(query);
            if (songs.isEmpty()) {
                System.out.println("No songs found matching \"" + query + "\".");
                return;
            }
            System.out.println("--- Search Results ---");
            for (Song song : songs) {
                System.out.println(song);
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void addNewSong() {
        try {
            System.out.print("Enter song title: ");
            if (!scanner.hasNextLine()) {
                System.out.println("No input available.");
                return;
            }
            String title = scanner.nextLine();

            System.out.print("Enter artist name: ");
            if (!scanner.hasNextLine()) {
                System.out.println("No input available.");
                return;
            }
            String artist = scanner.nextLine();

            System.out.print("Enter album name: ");
            if (!scanner.hasNextLine()) {
                System.out.println("No input available.");
                return;
            }
            String album = scanner.nextLine();

            int durationSeconds = readInt("Enter duration in seconds: ");

            Song song = new Song();
            song.setTitle(title);
            song.setArtistName(artist);
            song.setAlbumName(album);
            song.setDurationSeconds(durationSeconds);

            boolean success = songDao.addSong(song);
            if (success) {
                System.out.println("Song added successfully!");
            } else {
                System.out.println("Failed to add song.");
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}


