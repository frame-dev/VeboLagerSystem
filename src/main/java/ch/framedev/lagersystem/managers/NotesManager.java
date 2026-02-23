package ch.framedev.lagersystem.managers;

import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.classes.Note;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("deprecation")
public class NotesManager {

    private final Logger LOGGER = LogManager.getLogger(NotesManager.class);

    private final String TABLE = DatabaseManager.TABLE_NOTES;
    private static NotesManager instance;
    private final DatabaseManager databaseManager;

    // ==================== Cache ====================
    private final ConcurrentHashMap<String, Note> cache = new ConcurrentHashMap<>();
    private volatile List<Note> allNotesCache = null;
    private volatile long allNotesCacheTime = 0L;

    private NotesManager() {
        databaseManager = Main.databaseManager;
        createTable();
    }

    public static NotesManager getInstance() {
        if (instance == null) {
            instance = new NotesManager();
        }
        return instance;
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT NOT NULL," +
                "content VARCHAR(2555)," +
                "date TEXT" +
                ");";
        databaseManager.executeUpdate(sql);
    }

    public boolean exists(String title) {
        if (title == null) return false;
        // prefer cache
        if (cache.containsKey(title)) return true;

        String sql = "SELECT COUNT(*) FROM " + TABLE + " WHERE title = ?;";
        try (ResultSet resultSet = databaseManager.executePreparedQuery(sql, new Object[]{title})) {
            if (resultSet.next()) {
                int count = resultSet.getInt(1);
                return count > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    public boolean addNote(String title, String content) {
        if (exists(title)) {
            Main.logUtils.addLog("Notiz mit Titel '" + title + "' existiert bereits.");
            return false;
        }
        SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        String date = formatter.format(System.currentTimeMillis());
        String sql = "INSERT INTO " + TABLE + " (title, content, date) VALUES (?, ?, ?);";
        boolean result = databaseManager.executePreparedUpdate(sql, new Object[]{title, content, date});
        if (result) {
            // update cache
            Note n = new Note(title, content, date);
            cache.put(title, n);
            allNotesCache = null;
            allNotesCacheTime = 0L;
        }
        return result;
    }

    public boolean updateNote(String title, String content) {
        if (!exists(title)) {
            Main.logUtils.addLog("Notiz mit Titel '" + title + "' existiert nicht.");
            return false;
        }
        String sql = "UPDATE " + TABLE + " SET content = ? WHERE title = ?;";
        boolean result = databaseManager.executePreparedUpdate(sql, new Object[]{content, title});
        if (result) {
            // invalidate per-note cache entry so next read fetches fresh data
            cache.remove(title);
            allNotesCache = null;
            allNotesCacheTime = 0L;
        }
        return result;
    }

    public boolean deleteNote(String title) {
        if (!exists(title)) {
            Main.logUtils.addLog("Notiz mit Titel '" + title + "' existiert nicht.");
            return false;
        }
        String sql = "DELETE FROM " + TABLE + " WHERE title = ?;";
        boolean result = databaseManager.executePreparedUpdate(sql, new Object[]{title});
        if (result) {
            cache.remove(title);
            allNotesCache = null;
            allNotesCacheTime = 0L;
        }
        return result;
    }

    public List<Note> getAllNotes() {
        long now = System.currentTimeMillis();
        // 5 minutes
        long CACHE_TTL_MILLIS = 5 * 60 * 1000;
        if (allNotesCache != null && (now - allNotesCacheTime) < CACHE_TTL_MILLIS) {
            return allNotesCache;
        }

        List<Note> notes = new ArrayList<>();
        String sql = "SELECT * FROM " + TABLE + ";";
        try (ResultSet resultSet = databaseManager.executeQuery(sql)) {
            while (resultSet.next()) {
                String title = resultSet.getString("title");
                String content = resultSet.getString("content");
                String date = resultSet.getString("date");
                Note n = new Note(title, content, date);
                notes.add(n);
                // refresh per-note cache
                cache.put(title, n);
            }
            allNotesCache = Collections.unmodifiableList(notes);
            allNotesCacheTime = System.currentTimeMillis();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (Exception ex) {
            LOGGER.error("Error fetching all notes", ex);
        }
        return notes;
    }

    public Note getNoteByTitle(String title) {
        if (title == null) return null;
        // try cache first
        Note cached = cache.get(title);
        if (cached != null) return cached;

        String sql = "SELECT * FROM " + TABLE + " WHERE title = ?;";
        try (ResultSet resultSet = databaseManager.executePreparedQuery(sql, new Object[]{title})) {
            if (resultSet.next()) {
                String content = resultSet.getString("content");
                String date = resultSet.getString("date");
                Note n = new Note(title, content, date);
                cache.put(title, n);
                return n;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    /**
     * Clear both per-note and list caches immediately.
     */
    public void clearCache() {
        cache.clear();
        allNotesCache = null;
        allNotesCacheTime = 0L;
    }
}
