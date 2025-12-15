# How to Set Up and Run the Music Library

## Step 1: Create the Database

You need to **execute** the SQL script in MySQL, not just paste it. Choose one method:

### Option A: MySQL Command Line

1. Open **Command Prompt** or **PowerShell**
2. Type: `mysql -u root -p`
3. Enter password when prompted: `1405`
4. Once connected, copy and paste the ENTIRE contents of `sql/music_library_schema.sql`
5. Press Enter
6. You should see messages like "Query OK" for each command

### Option B: MySQL Workbench (Easier!)

1. Open **MySQL Workbench**
2. Connect to your MySQL server (username: root, password: 1405)
3. Click on **File → Open SQL Script**
4. Navigate to `sql/music_library_schema.sql` and open it
5. Click the **Execute** button (⚡ lightning bolt icon) or press `Ctrl+Shift+Enter`
6. You should see "Success" messages in the output panel

### Option C: Command Line (One Command)

Open PowerShell in this folder and run:
```powershell
mysql -u root -p1405 < sql/music_library_schema.sql
```

## Step 2: Verify Database Was Created

In MySQL, run:
```sql
SHOW DATABASES;
USE music_library;
SHOW TABLES;
SELECT * FROM songs;
```

You should see the database `music_library` and 5 sample songs.

## Step 3: Run the Java Application

In PowerShell, from this folder:
```powershell
mvn exec:java
```

## Troubleshooting

**If you get "Access denied" error:**
- Check your MySQL password is correct (1405)
- Make sure MySQL service is running

**If you get "Unknown database" error:**
- The SQL script didn't execute properly - try Method B (MySQL Workbench)

**If you get connection errors:**
- Make sure MySQL is running on port 3306
- Check `src/main/resources/db.properties` has correct password

