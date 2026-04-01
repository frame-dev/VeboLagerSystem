package ch.framedev.lagersystem.managers;

import ch.framedev.lagersystem.managers.LogManager.Log;
import ch.framedev.lagersystem.managers.LogManager.LogLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogManagerTest extends ManagerTestSupport {

    // =========================================================================
    // createLog / getLogById
    // =========================================================================

    @Test
    @DisplayName("createLog and getLogById: persists and retrieves a log entry")
    void createLogAndGetById_works() {
        LogManager manager = LogManager.getInstance();

        assertTrue(manager.createLog(LogLevel.INFO, "Hello Test"));

        List<Log> all = manager.getAllLogs();
        assertEquals(1, all.size());

        Log stored = all.get(0);
        assertEquals("INFO", stored.level());
        assertEquals("Hello Test", stored.message());
        assertNotNull(stored.timestamp());

        Log byId = manager.getLogById(stored.id());
        assertNotNull(byId);
        assertEquals(stored.id(), byId.id());
        assertEquals("Hello Test", byId.message());
    }

    @Test
    @DisplayName("createLog(Log): explicit log record is persisted")
    void createLog_explicitRecord_persisted() {
        LogManager manager = LogManager.getInstance();
        Log record = new Log("01.01.2026 10:00:00", "DEBUG", "explicit record");

        assertTrue(manager.createLog(record));
        assertEquals(1, manager.getAllLogs().size());
    }

    // =========================================================================
    // getAllLogs
    // =========================================================================

    @Test
    @DisplayName("getAllLogs: returns empty list when no logs exist")
    void getAllLogs_empty_returnsEmptyList() {
        assertTrue(LogManager.getInstance().getAllLogs().isEmpty());
    }

    @Test
    @DisplayName("getAllLogs: returns all inserted log entries")
    void getAllLogs_returnsAllEntries() {
        LogManager manager = LogManager.getInstance();
        manager.createLog(LogLevel.INFO, "msg1");
        manager.createLog(LogLevel.WARNING, "msg2");
        manager.createLog(LogLevel.ERROR, "msg3");

        assertEquals(3, manager.getAllLogs().size());
    }

    // =========================================================================
    // getLogsByLevel
    // =========================================================================

    @Test
    @DisplayName("getLogsByLevel: filters correctly by level")
    void getLogsByLevel_filtersCorrectly() {
        LogManager manager = LogManager.getInstance();
        manager.createLog(LogLevel.INFO, "info msg");
        manager.createLog(LogLevel.ERROR, "error msg");
        manager.createLog(LogLevel.ERROR, "another error");

        List<Log> errors = manager.getLogsByLevel(LogLevel.ERROR);
        assertEquals(2, errors.size());
        assertTrue(errors.stream().allMatch(l -> "ERROR".equals(l.level())));

        List<Log> infos = manager.getLogsByLevel(LogLevel.INFO);
        assertEquals(1, infos.size());
    }

    @Test
    @DisplayName("getLogsByLevel: returns empty list when no matching level exists")
    void getLogsByLevel_noMatch_returnsEmpty() {
        LogManager manager = LogManager.getInstance();
        manager.createLog(LogLevel.INFO, "info only");

        assertTrue(manager.getLogsByLevel(LogLevel.DEBUG).isEmpty());
    }

    // =========================================================================
    // searchLogs
    // =========================================================================

    @Test
    @DisplayName("searchLogs: finds logs by substring match")
    void searchLogs_substringMatch() {
        LogManager manager = LogManager.getInstance();
        manager.createLog(LogLevel.INFO, "Article inserted successfully");
        manager.createLog(LogLevel.INFO, "User logged in");
        manager.createLog(LogLevel.WARNING, "Article stock low");

        List<Log> results = manager.searchLogs("Article");
        assertEquals(2, results.size());
    }

    @Test
    @DisplayName("searchLogs: null search term returns empty list")
    void searchLogs_nullTerm_returnsEmpty() {
        LogManager manager = LogManager.getInstance();
        manager.createLog(LogLevel.INFO, "some log");

        assertTrue(manager.searchLogs(null).isEmpty());
    }

    @Test
    @DisplayName("searchLogs: no match returns empty list")
    void searchLogs_noMatch_returnsEmpty() {
        LogManager manager = LogManager.getInstance();
        manager.createLog(LogLevel.INFO, "Hello world");

        assertTrue(manager.searchLogs("xyz123notfound").isEmpty());
    }

    // =========================================================================
    // deleteLog
    // =========================================================================

    @Test
    @DisplayName("deleteLog: removes the entry; getLogById returns null afterwards")
    void deleteLog_removesEntry() {
        LogManager manager = LogManager.getInstance();
        manager.createLog(LogLevel.INFO, "to be deleted");

        Log log = manager.getAllLogs().get(0);
        assertTrue(manager.deleteLog(log.id()));
        assertNull(manager.getLogById(log.id()));
        assertTrue(manager.getAllLogs().isEmpty());
    }

    // =========================================================================
    // clearAllLogs
    // =========================================================================

    @Test
    @DisplayName("clearAllLogs: empties the log table")
    void clearAllLogs_emptiesTable() {
        LogManager manager = LogManager.getInstance();
        manager.createLog(LogLevel.INFO, "a");
        manager.createLog(LogLevel.ERROR, "b");
        manager.createLog(LogLevel.DEBUG, "c");

        assertTrue(manager.clearAllLogs());
        assertTrue(manager.getAllLogs().isEmpty());
    }

    // =========================================================================
    // getLogCountByLevel / getTotalLogCount
    // =========================================================================

    @Test
    @DisplayName("getLogCountByLevel: returns correct count per level")
    void getLogCountByLevel_correctCount() {
        LogManager manager = LogManager.getInstance();
        manager.createLog(LogLevel.INFO, "i1");
        manager.createLog(LogLevel.INFO, "i2");
        manager.createLog(LogLevel.ERROR, "e1");

        assertEquals(2, manager.getLogCountByLevel(LogLevel.INFO));
        assertEquals(1, manager.getLogCountByLevel(LogLevel.ERROR));
        assertEquals(0, manager.getLogCountByLevel(LogLevel.DEBUG));
    }

    @Test
    @DisplayName("getTotalLogCount: equals sum of all inserted logs")
    void getTotalLogCount_equalsTotalInserted() {
        LogManager manager = LogManager.getInstance();
        manager.createLog(LogLevel.INFO, "a");
        manager.createLog(LogLevel.WARNING, "b");
        manager.createLog(LogLevel.ERROR, "c");

        assertEquals(3, manager.getTotalLogCount());
    }

    // =========================================================================
    // deleteOldLogs
    // =========================================================================

    @Test
    @DisplayName("deleteOldLogs: does not delete recently created logs")
    void deleteOldLogs_recentLogs_notDeleted() {
        LogManager manager = LogManager.getInstance();
        manager.createLog(LogLevel.INFO, "recent log");

        int deleted = manager.deleteOldLogs(30);

        assertEquals(0, deleted);
        assertEquals(1, manager.getAllLogs().size());
    }

    // =========================================================================
    // getLogById: unknown id
    // =========================================================================

    @Test
    @DisplayName("getLogById: returns null for non-existent id")
    void getLogById_unknown_returnsNull() {
        assertNull(LogManager.getInstance().getLogById(999999));
    }
}
