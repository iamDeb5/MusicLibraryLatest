# Online Music Library - Complete Usage Guide

## 🚀 Server Status
✅ **Server is running and fully operational**
- **URL:** http://localhost:8080
- **Port:** 8080
- **Status:** All features tested and working

---

## 📋 How to Start the Server

### Option 1: Start in Foreground (with logs visible)
```powershell
cd C:\Users\DEBOJYOTI\Documents\CursorMusicLibrary
mvn -DskipTests exec:java
```
Output: "Server running at: http://localhost:8080"

### Option 2: Start as Background Job
```powershell
$job = Start-Job -ScriptBlock { 
    cd C:\Users\DEBOJYOTI\Documents\CursorMusicLibrary
    mvn -DskipTests exec:java 
}
Start-Sleep -Seconds 5  # Wait for server to start
```

### Option 3: Build Then Run
```powershell
mvn -DskipTests clean package
mvn -DskipTests exec:java
```

---

## 🌐 Accessing the Web Interface

Open your browser and navigate to:
```
http://localhost:8080
```

### Pages Available:

1. **Home/Songs Page** (`/`)
   - View all songs in the library
   - Create new songs
   - Delete songs from the library

2. **Playlists Page** (`/playlists.html`)
   - View all playlists
   - Create new playlists
   - Manage songs in playlists
   - View playlist details

---

## 🎵 Songs Management

### Via Web UI (Recommended for Users)

**Access:** http://localhost:8080

**Features:**
- **Create Song:**
  - Click "Add New Song" button
  - Fill in: Title, Artist Name, Album Name, Duration (seconds)
  - Click "Create Song"

- **View Songs:**
  - Songs are displayed in a table
  - Shows: ID, Title, Artist, Album, Duration
  - List updates in real-time

- **Delete Song:**
  - Click the "Delete" button next to any song
  - Song is immediately removed from library
  - All associated playlist entries are also deleted

### Via REST API (for developers)

**Base URL:** `http://localhost:8080/api/songs`

#### 1. Get All Songs
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/songs" -Method Get
```

#### 2. Create a Song
```powershell
$body = @{
    title = "Song Title"
    artistName = "Artist Name"
    albumName = "Album Name"
    durationSeconds = 200
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/songs" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body
```

**Response:**
```json
{
    "id": 1,
    "title": "Song Title",
    "artistName": "Artist Name",
    "albumName": "Album Name",
    "durationSeconds": 200,
    "audioFilePath": null
}
```

#### 3. Delete a Song
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/songs/1" -Method Delete
```

---

## 🎯 Playlists Management

### Via Web UI (Recommended for Users)

**Access:** http://localhost:8080/playlists.html

**Features:**

#### Create Playlist
1. Click "Create New Playlist"
2. Enter:
   - Playlist name (required)
   - Description (optional)
3. Click "Create"
4. New playlist appears in the list

#### View Playlists
- All playlists displayed with:
  - Name
  - Description
  - Creation date
  - Number of songs

#### Add Song to Playlist
1. Click on any playlist to open it
2. Click "Add Song" button
3. Select a song from the dropdown menu
4. Click "Add"
5. Song is added to the playlist in order

#### Remove Song from Playlist
1. Click on the playlist to view it
2. Find the song in the playlist
3. Click the "Remove" button next to the song
4. Song is removed from the playlist (not from library)

#### Delete Playlist
1. Click the "Delete" button on the playlist card
2. Playlist is permanently deleted
3. Songs remain in the library

#### View Full Playlist
1. Click on any playlist name to open it
2. See all songs in the playlist with their details:
   - Position in playlist
   - Title, Artist, Album
   - Duration

### Via REST API (for developers)

**Base URL:** `http://localhost:8080/api/playlists`

#### 1. Get All Playlists
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/playlists" -Method Get
```

**Response:**
```json
[
    {
        "id": 1,
        "name": "My Favorites",
        "description": "Songs I love",
        "createdAt": 1765877731000,
        "updatedAt": 1765877731000,
        "songCount": 3
    }
]
```

#### 2. Get Playlist by ID (with all songs)
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/playlists/1" -Method Get
```

**Response:**
```json
{
    "id": 1,
    "name": "My Favorites",
    "description": "Songs I love",
    "createdAt": 1765877731000,
    "updatedAt": 1765877731000,
    "songs": [
        {
            "id": 1,
            "title": "Song Title",
            "artistName": "Artist Name",
            "albumName": "Album Name",
            "durationSeconds": 200,
            "audioFilePath": null
        }
    ],
    "songCount": 1
}
```

#### 3. Create a Playlist
```powershell
$body = @{
    name = "My Playlist"
    description = "A great playlist"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/playlists" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body
```

#### 4. Add Song to Playlist
```powershell
$body = @{
    songId = 1
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/playlists/1" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body
```

**Response:**
```json
{
    "success": true,
    "message": "Song added to playlist"
}
```

#### 5. Remove Song from Playlist
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/playlists/1/1" -Method Delete
```

**Response:**
```json
{
    "success": true,
    "message": "Song removed from playlist"
}
```

#### 6. Update Playlist
```powershell
$body = @{
    name = "Updated Name"
    description = "Updated description"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/playlists/1" `
    -Method Put `
    -ContentType "application/json" `
    -Body $body
```

#### 7. Delete Playlist
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/playlists/1" -Method Delete
```

---

## 📊 Quick Test Commands

### Create a Test Song
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/songs" -Method Post `
    -ContentType "application/json" `
    -Body '{"title":"Test Song","artistName":"Test Artist","albumName":"Test Album","durationSeconds":180}'
```

### Create a Test Playlist
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/playlists" -Method Post `
    -ContentType "application/json" `
    -Body '{"name":"Test Playlist","description":"A test playlist"}'
```

### Get All Songs
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/songs" -Method Get | ConvertTo-Json
```

### Get All Playlists
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/playlists" -Method Get | ConvertTo-Json
```

---

## 🗄️ Database Information

- **Database:** `music_library`
- **Host:** `localhost`
- **Port:** `3306`
- **Tables:**
  - `songs` - All songs in library
  - `artists` - Artist information
  - `albums` - Album information
  - `playlists` - Playlist metadata
  - `playlist_songs` - Songs in each playlist (with ordering)

**Connection:** `jdbc:mysql://localhost:3306/music_library`
**Credentials:** See `src/main/resources/db.properties`

---

## 🔧 Project Structure

```
CursorMusicLibrary/
├── src/main/java/com/example/musiclibrary/
│   ├── MusicLibraryApp.java       (Main application class)
│   ├── WebServer.java             (Jetty server setup)
│   ├── SongsServlet.java          (Songs REST endpoints)
│   ├── PlaylistServlet.java       (Playlists REST endpoints)
│   ├── StaticFileServlet.java     (Static files serving)
│   ├── dao/
│   │   └── SongDao.java           (Song database operations)
│   │   └── PlaylistDao.java       (Playlist database operations)
│   ├── db/
│   │   └── DatabaseConnection.java (Database connection & setup)
│   └── model/
│       ├── Song.java              (Song model)
│       └── Playlist.java          (Playlist model)
├── src/main/resources/
│   ├── db.properties              (Database configuration)
│   └── webapp/
│       ├── index.html             (Songs management UI)
│       ├── playlists.html         (Playlists management UI)
│       ├── app.js                 (Frontend logic)
│       └── style.css              (Styling)
├── sql/
│   └── music_library_schema.sql   (Database schema)
├── pom.xml                        (Maven configuration)
└── target/                        (Compiled files)
```

---

## ✅ Verified Features

- ✅ Create, Read, Delete Songs
- ✅ Create, Read, Update, Delete Playlists
- ✅ Add songs to playlists
- ✅ Remove songs from playlists
- ✅ View playlists with all songs
- ✅ Automatic database table creation
- ✅ REST API fully functional
- ✅ Web UI working smoothly
- ✅ Real-time list updates
- ✅ Proper error handling

---

## 🚫 Stopping the Server

### If running in foreground:
Press `Ctrl + C` in the terminal

### If running as background job:
```powershell
# Find and stop the job
Get-Job | Stop-Job
Get-Job | Remove-Job
```

Or find the Java process:
```powershell
Get-Process java | Stop-Process -Force
```

---

## 📝 Notes

- Database tables are auto-created at startup if missing
- All timestamps are stored in milliseconds (Unix time)
- Songs are permanently deleted when removed
- Playlists can be empty (0 songs)
- Playlist order is maintained (songs have a position)
- All API responses are in JSON format

---

## 🎬 Example Workflow

1. **Start Server:**
   ```powershell
   mvn -DskipTests exec:java
   ```

2. **Open Browser:**
   Navigate to http://localhost:8080

3. **Create Songs:**
   - Click "Add New Song"
   - Add 3-4 songs with different artists

4. **Switch to Playlists:**
   - Click "Playlists" in the navigation

5. **Create Playlist:**
   - Click "Create New Playlist"
   - Name it "My Favorites"

6. **Add Songs to Playlist:**
   - Click on the playlist
   - Click "Add Song"
   - Select songs from dropdown
   - They appear in order

7. **Manage:**
   - Remove songs with "Remove" button
   - Delete playlist with "Delete Playlist" button
   - Songs stay in library when removed from playlist

---

## 🆘 Troubleshooting

**Server won't start?**
- Check MySQL is running: `Invoke-RestMethod http://localhost:3306`
- Check port 8080 isn't in use: `netstat -ano | findstr :8080`

**Database error?**
- Verify MySQL credentials in `src/main/resources/db.properties`
- Tables auto-create on first run

**API returns 404?**
- Make sure server is running on port 8080
- Check endpoint URL spelling
- Verify JSON body format

**Songs/Playlists not showing?**
- Refresh browser page
- Check browser console for errors (F12)
- Verify database connection

---

Last Updated: December 16, 2025
