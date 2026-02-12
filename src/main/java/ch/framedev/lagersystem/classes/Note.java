package ch.framedev.lagersystem.classes;

/**
 * Simple domain model for a personal note.
 * Stores title, content, and a formatted date string.
 */
public class Note {

    private String title, content, date;

    /**
     * Creates a new note instance.
     *
     * @param title   note title
     * @param content note content/body
     * @param date    formatted date string (e.g. "dd.MM.yyyy HH:mm:ss")
     */
    public Note(String title, String content, String date) {
        this.title = title;
        this.content = content;
        this.date = date;
    }

    /**
     * @return note title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return note content/body
     */
    public String getContent() {
        return content;
    }

    /**
     * @return formatted date string
     */
    public String getDate() {
        return date;
    }

    /**
     * Updates the note title.
     *
     * @param title new title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Updates the note content/body.
     *
     * @param content new content
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Updates the note date string.
     *
     * @param date new formatted date string
     */
    public void setDate(String date) {
        this.date = date;
    }
}
