## Online Music Library (Java + MySQL + Web Frontend)

A beautiful web-based online music library (like a tiny version of Prime Music) built with:
- **Backend**: Java, Maven, MySQL, Jetty Server
- **Frontend**: HTML5, CSS3, JavaScript
- **Features**: Browse songs, search, add new songs

### 1. Prerequisites
- **Java**: JDK 8 or newer installed and on PATH (`java -version`)
- **Maven**: Installed and on PATH (`mvn -v`)
- **MySQL Server**: Running locally on port **3306**

### 2. Set up MySQL database
1. Open a MySQL client (Workbench, command line, etc.)
2. Run the SQL script in `sql/music_library_schema.sql`:
   - This creates the `music_library` database
   - Creates the `artists`, `albums`, and `songs` tables
   - Inserts some sample data

### 3. Configure database connection
Edit `src/main/resources/db.properties` and set:

```properties
db.url=jdbc:mysql://localhost:3306/music_library?useSSL=false&serverTimezone=UTC
db.user=YOUR_MYSQL_USERNAME
db.password=YOUR_MYSQL_PASSWORD
```

For example, if your MySQL user is `root` with password `1234`:

```properties
db.url=jdbc:mysql://localhost:3306/music_library?useSSL=false&serverTimezone=UTC
db.user=root
db.password=1234
```

### 4. Build the project
From the project root (`CursorMusicLibrary`), run:

```bash
mvn clean compile
```

You should see **BUILD SUCCESS** if everything is correct.

### 5. Run the web application

```bash
mvn exec:java
```

The server will start on **http://localhost:8080**

You should see:
```
✓ Database connected successfully!
========================================
🎵 Online Music Library Web Server
========================================
Server running at: http://localhost:8080
Open your browser and visit the URL above
Press Ctrl+C to stop the server
========================================
```

### 6. Open in your browser

Open your web browser and go to:
```
http://localhost:8080
```

You'll see a beautiful web interface where you can:
- **Browse all songs** - View your entire music collection
- **Search songs** - Find songs by title
- **Add new songs** - Add songs with artist, album, and duration

### Features
- 🎨 Modern, responsive web UI
- 🔍 Real-time search
- ➕ Add new songs with form validation
- 📱 Mobile-friendly design
- ⚡ Fast REST API backend


