package com.example.musiclibrary.model;

public class Song {

    private int id;
    private String title;
    private String artistName;
    private String albumName;
    private int durationSeconds;
    private String audioFilePath; // Path to audio file

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public String getAlbumName() {
        return albumName;
    }

    public void setAlbumName(String albumName) {
        this.albumName = albumName;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getAudioFilePath() {
        return audioFilePath;
    }

    public void setAudioFilePath(String audioFilePath) {
        this.audioFilePath = audioFilePath;
    }
    @Override
    public String toString() {
        int minutes = durationSeconds / 60;
        int seconds = durationSeconds % 60;
        String durationFormatted = String.format("%d:%02d", minutes, seconds);
        return "Song{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", artist='" + artistName + '\'' +
                ", album='" + albumName + '\'' +
                ", duration=" + durationFormatted +
                '}';
    }
}


