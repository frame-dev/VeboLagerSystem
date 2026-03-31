package ch.framedev.lagersystem.classes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class NoteTest {

    @Test
    @DisplayName("constructor: stores title, content and date")
    void constructor_storesFields() {
        Note note = new Note("Titel", "Inhalt", "31.03.2026 21:00:00");

        assertEquals("Titel", note.getTitle());
        assertEquals("Inhalt", note.getContent());
        assertEquals("31.03.2026 21:00:00", note.getDate());
    }

    @Test
    @DisplayName("constructor: accepts null values")
    void constructor_acceptsNullValues() {
        Note note = new Note(null, null, null);

        assertNull(note.getTitle());
        assertNull(note.getContent());
        assertNull(note.getDate());
    }

    @Test
    @DisplayName("setters: update all fields")
    void setters_updateAllFields() {
        Note note = new Note("Alt", "Alt", "Alt");

        note.setTitle("Neu");
        note.setContent("Neuer Inhalt");
        note.setDate("01.04.2026 08:15:00");

        assertEquals("Neu", note.getTitle());
        assertEquals("Neuer Inhalt", note.getContent());
        assertEquals("01.04.2026 08:15:00", note.getDate());
    }
}
