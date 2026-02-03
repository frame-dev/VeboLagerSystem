package ch.framedev.lagersystem.managers;

import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.utils.Note;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class NotesManager {

    private final String TABLE = "notes";
    private static NotesManager instance;
    private DatabaseManager databaseManager;

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
        return databaseManager.executePreparedUpdate(sql, new Object[]{title, content, date});
    }

    public boolean updateNote(String title, String content) {
        if(!exists(title)) {
            Main.logUtils.addLog("Notiz mit Titel '" + title + "' existiert nicht.");
            return false;
        }
        String sql = "UPDATE " + TABLE + " SET content = ? WHERE title = ?;";
        return databaseManager.executePreparedUpdate(sql, new Object[]{content, title});
    }

    public boolean deleteNote(String title) {
        if(!exists(title)) {
            Main.logUtils.addLog("Notiz mit Titel '" + title + "' existiert nicht.");
            return false;
        }
        String sql = "DELETE FROM " + TABLE + " WHERE title = ?;";
        return databaseManager.executePreparedUpdate(sql, new Object[]{title});
    }
    
    public List<Note> getAllNotes() {
        List<Note> notes = new ArrayList<>();
        String sql = "SELECT * FROM " + TABLE + ";";
        try(ResultSet resultSet = databaseManager.executeQuery(sql)) {
            while (resultSet.next()) {
                String title = resultSet.getString("title");
                String content = resultSet.getString("content");
                String date = resultSet.getString("date");
                notes.add(new Note(title, content, date));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return notes;
    }

    public Note getNoteByTitle(String title) {
        String sql = "SELECT * FROM " + TABLE + " WHERE title = ?;";
        try (ResultSet resultSet = databaseManager.executePreparedQuery(sql, new Object[]{title})) {
            if (resultSet.next()) {
                String content = resultSet.getString("content");
                String date = resultSet.getString("date");
                return new Note(title, content, date);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
