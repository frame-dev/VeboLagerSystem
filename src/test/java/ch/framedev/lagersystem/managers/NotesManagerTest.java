package ch.framedev.lagersystem.managers;

import ch.framedev.lagersystem.classes.Note;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotesManagerTest extends ManagerTestSupport {

    @Test
    @DisplayName("add/get/update/delete: note CRUD works")
    void noteCrud_works() {
        NotesManager manager = NotesManager.getInstance();

        assertTrue(manager.addNote("Idee", "Erster Text"));
        assertTrue(manager.exists("Idee"));

        Note note = manager.getNoteByTitle("Idee");
        assertNotNull(note);
        assertEquals("Erster Text", note.getContent());

        assertTrue(manager.updateNote("Idee", "Aktualisiert"));
        assertEquals("Aktualisiert", manager.getNoteByTitle("Idee").getContent());

        assertTrue(manager.deleteNote("Idee"));
        assertFalse(manager.exists("Idee"));
        assertNull(manager.getNoteByTitle("Idee"));
    }

    @Test
    @DisplayName("getAllNotes: returns inserted notes")
    void getAllNotes_returnsInsertedNotes() {
        NotesManager manager = NotesManager.getInstance();
        manager.addNote("N1", "A");
        manager.addNote("N2", "B");

        List<Note> notes = manager.getAllNotes();

        assertEquals(2, notes.size());
        assertTrue(notes.stream().anyMatch(note -> "N1".equals(note.getTitle())));
        assertTrue(notes.stream().anyMatch(note -> "N2".equals(note.getTitle())));
    }

    @Test
    @DisplayName("normalizeTextNotNull: rejects blank and duplicate titles")
    void normalizeTextNotNull_rejectsBlankAndDuplicateTitles() {
        NotesManager manager = NotesManager.getInstance();
        manager.addNote("Vorhanden", "Text");

        assertFalse(manager.normalizeTextNotNull("   "));
        assertFalse(manager.normalizeTextNotNull("Vorhanden"));
        assertTrue(manager.normalizeTextNotNull("Neu"));
    }
}
