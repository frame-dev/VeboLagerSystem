package ch.framedev.lagersystem.managers;

import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.classes.Note;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The NotesManager class is responsible for managing notes in the LagerSystem application. It provides methods to create, read, update, and delete notes from the database. The class implements a singleton pattern to ensure that only one instance of the manager exists throughout the application. It also includes caching mechanisms to improve performance when accessing notes, with both per-note caching and a cache for the list of all notes. The class uses prepared statements for database operations to prevent SQL injection and ensure efficient query execution. Logging is implemented to track operations and errors related to note management.
 *
 * @author framedev
 */
@SuppressWarnings("deprecation")
public class NotesManager {

    private final Logger LOGGER = LogManager.getLogger(NotesManager.class);

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss", Locale.ROOT);

    private final String TABLE = DatabaseManager.TABLE_NOTES;
    private static volatile NotesManager instance;
    private final DatabaseManager databaseManager;

    // ==================== Cache ====================
    private final ConcurrentHashMap<String, Note> cache = new ConcurrentHashMap<>();
    private volatile List<Note> allNotesCache = null;
    private volatile long allNotesCacheTime = 0L;

    private static String normalizeTitle(String title) {
        if (title == null) return null;
        String t = title.trim();
        return t.isEmpty() ? null : t;
    }

    private void invalidateAllNotesCache() {
        allNotesCache = null;
        allNotesCacheTime = 0L;
    }

    private NotesManager() {
        databaseManager = Main.databaseManager;
        createTable();
    }

    /**
     * Returns the singleton instance of NotesManager. If the instance does not exist yet, it will be created.
     *
     * @return The singleton instance of NotesManager.
     */
    public static NotesManager getInstance() {
        NotesManager local = instance;
        if (local == null) {
            synchronized (NotesManager.class) {
                local = instance;
                if (local == null) {
                    local = new NotesManager();
                    instance = local;
                }
            }
        }
        return local;
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT NOT NULL UNIQUE," +
                "content VARCHAR(2555)," +
                "date TEXT" +
                ");";
        databaseManager.executeUpdate(sql);
    }

    /**
     * Checks if a note with the given title exists in the database. The method first checks the cache for the existence of the note, and if it is not found, it queries the database. If the note exists in the database, it is added to the cache before returning true. If the note does not exist, the method returns false. If an error occurs during the database query, a RuntimeException is thrown.
     *
     * @param title The title of the note to check for existence. This must not be null.
     * @return true if a note with the given title exists, false if no such note exists or if an error occurs during the check.
     */
    public boolean exists(String title) {
        String t = normalizeTitle(title);
        if (t == null) return false;
        // prefer cache
        if (cache.containsKey(t)) return true;

        String sql = "SELECT 1 FROM " + TABLE + " WHERE title = ? LIMIT 1;";
        try (ResultSet resultSet = databaseManager.executePreparedQuery(sql, new Object[]{t})) {
            return resultSet.next();
        } catch (SQLException e) {
            LOGGER.error("Error while checking if note exists: {}", t, e);
            Main.logUtils.addLog("Fehler beim Prüfen ob Notiz existiert: " + t);
            return false;
        }
    }

    /**
     * Adds a new note to the database with the given title and content. If a note with the same title already exists, the method will return false and not perform the insertion. If the insertion is successful, the new note is added to the cache and any existing cache for the list of all notes is invalidated to ensure that subsequent calls to getAllNotes() will fetch fresh data from the database.
     *
     * @param title   The title of the note to add. This must be unique and not null.
     * @param content The content of the note to add. This can be null if you want to create an empty note.
     * @return true if the note was successfully added, false if a note with the same title already exists or if the insertion failed.
     */
    public boolean addNote(String title, String content) {
        String t = normalizeTitle(title);
        if(!normalizeTextNotNull(title)) return false;
        String date = DATE_FORMAT.format(LocalDateTime.now());
        String sql = "INSERT INTO " + TABLE + " (title, content, date) VALUES (?, ?, ?);";
        boolean result = databaseManager.executePreparedUpdate(sql, new Object[]{t, content, date});
        if (result) {
            // update cache
            Note n = new Note(t, content, date);
            cache.put(t, n);
            invalidateAllNotesCache();
        }
        return result;
    }

    /**
     * Updates the content of an existing note identified by its title. If the note does not exist, a log entry is added and the method returns false. If the update is successful, the corresponding cache entry is invalidated to ensure that subsequent reads will fetch the updated data from the database.
     *
     * @param title   The title of the note to update. This must not be null and must correspond to an existing note in the database.
     * @param content The new content to set for the note. This can be null if you want to clear the content of the note.
     * @return true if the note was successfully updated, false if the note does not exist or if the update operation failed.
     */
    public boolean updateNote(String title, String content) {
        String t = normalizeTitle(title);
        if(!normalizeTextNotNull(title)) return false;
        String sql = "UPDATE " + TABLE + " SET content = ? WHERE title = ?;";
        boolean result = databaseManager.executePreparedUpdate(sql, new Object[]{content, t});
        if (result) {
            // invalidate per-note cache entry so next read fetches fresh data
            cache.remove(t);
            invalidateAllNotesCache();
        }
        return result;
    }

    /**
     * Deletes a note from the database based on its title. If the note does not exist, a log entry is added and the method returns false. If the deletion is successful, the corresponding cache entry is removed to ensure that subsequent reads will not return stale data from the cache.
     *
     * @param title The title of the note to delete. This must not be null and must correspond to an existing note in the database.
     * @return true if the note was successfully deleted, false if the note does not exist or if the deletion operation failed.
     */
    public boolean deleteNote(String title) {
        String t = normalizeTitle(title);
        if(!normalizeTextNotNull(title)) return false;
        String sql = "DELETE FROM " + TABLE + " WHERE title = ?;";
        boolean result = databaseManager.executePreparedUpdate(sql, new Object[]{t});
        if (result) {
            cache.remove(t);
            invalidateAllNotesCache();
        }
        return result;
    }

    public List<Note> getAllNotes() {
        long now = System.currentTimeMillis();
        final long CACHE_TTL_MILLIS = 5 * 60 * 1000;
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
            LOGGER.error("Error fetching all notes", e);
            Main.logUtils.addLog("Fehler beim Laden aller Notizen");
            return List.of();
        } catch (Exception ex) {
            LOGGER.error("Error fetching all notes", ex);
            Main.logUtils.addLog("Fehler beim Laden aller Notizen");
            return List.of();
        }
        return notes;
    }

    /**
     * Retrieves a note by its title. The method first checks the cache for the note, and if it is not found, it queries the database. If the note exists in the database, it is added to the cache before being returned. If the note does not exist, the method returns null.
     *
     * @param title The title of the note to retrieve. This must not be null.
     * @return The Note object corresponding to the given title, or null if no such note exists in the database. If an error occurs during retrieval, a RuntimeException is thrown.
     */
    public Note getNoteByTitle(String title) {
        String t = normalizeTitle(title);
        if (t == null) return null;
        // try cache first
        Note cached = cache.get(t);
        if (cached != null) return cached;

        String sql = "SELECT * FROM " + TABLE + " WHERE title = ?;";
        try (ResultSet resultSet = databaseManager.executePreparedQuery(sql, new Object[]{t})) {
            if (resultSet.next()) {
                String content = resultSet.getString("content");
                String date = resultSet.getString("date");
                Note n = new Note(t, content, date);
                cache.put(t, n);
                return n;
            }
        } catch (SQLException e) {
            LOGGER.error("Error while retrieving note by title: {}", t, e);
            Main.logUtils.addLog("Fehler beim Laden der Notiz: " + t);
            return null;
        }
        return null;
    }

    /**
     * Clear both per-note and list caches immediately.
     */
    public void clearCache() {
        cache.clear();
        invalidateAllNotesCache();
    }


    public boolean normalizeTextNotNull(String title) {
        String t = normalizeTitle(title);
        if (t == null) {
            Main.logUtils.addLog("Notiz-Titel ist leer oder ungültig.");
            return false;
        }
        if (exists(t)) {
            Main.logUtils.addLog("Notiz mit Titel '" + t + "' existiert bereits.");
            return false;
        }
        return true;
    }
}
