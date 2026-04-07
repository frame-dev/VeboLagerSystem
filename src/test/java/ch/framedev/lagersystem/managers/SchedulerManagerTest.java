package ch.framedev.lagersystem.managers;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SchedulerManagerTest extends ManagerTestSupport {

    // Subclass @AfterEach runs before the parent's, so the scheduler is shut
    // down and its singleton cleared before the parent closes the DB.
    @AfterEach
    void tearDownScheduler() throws Exception {
        resetSchedulerSingleton();
    }

    // -------------------------------------------------------------------------
    // Reflection helpers
    // -------------------------------------------------------------------------

    private void resetSchedulerSingleton() throws Exception {
        Field f = SchedulerManager.class.getDeclaredField("instance");
        f.setAccessible(true);
        SchedulerManager current = (SchedulerManager) f.get(null);
        if (current != null) {
            current.shutdown();
        }
        f.set(null, null);
    }

    private ScheduledExecutorService executor(SchedulerManager sm) throws Exception {
        Field f = SchedulerManager.class.getDeclaredField("executor");
        f.setAccessible(true);
        return (ScheduledExecutorService) f.get(sm);
    }

    private ScheduledFuture<?> stockCheckTask(SchedulerManager sm) throws Exception {
        Field f = SchedulerManager.class.getDeclaredField("stockCheckTask");
        f.setAccessible(true);
        return (ScheduledFuture<?>) f.get(sm);
    }

    private ScheduledFuture<?> warningDisplayTask(SchedulerManager sm) throws Exception {
        Field f = SchedulerManager.class.getDeclaredField("warningDisplayTask");
        f.setAccessible(true);
        return (ScheduledFuture<?>) f.get(sm);
    }

    private ScheduledFuture<?> autoImportTask(SchedulerManager sm) throws Exception {
        Field f = SchedulerManager.class.getDeclaredField("autoImportTask");
        f.setAccessible(true);
        return (ScheduledFuture<?>) f.get(sm);
    }

    // =========================================================================
    // Singleton
    // =========================================================================

    @Test
    @DisplayName("getInstance: always returns the same instance")
    void getInstance_returnsSameInstance() {
        SchedulerManager a = SchedulerManager.getInstance();
        SchedulerManager b = SchedulerManager.getInstance();
        assertSame(a, b);
    }

    // =========================================================================
    // startScheduledStockCheck – valid intervals
    // =========================================================================

    @Test
    @DisplayName("startScheduledStockCheck: executor is running and task is active after start")
    void startScheduledStockCheck_schedulesTask() throws Exception {
        SchedulerManager sm = SchedulerManager.getInstance();

        sm.startScheduledStockCheck(1, TimeUnit.HOURS);

        ScheduledExecutorService exec = executor(sm);
        ScheduledFuture<?> task = stockCheckTask(sm);
        assertNotNull(exec, "Executor should be created after start");
        assertFalse(exec.isShutdown(), "Executor should not be shut down after start");
        assertNotNull(task, "stockCheckTask should be non-null after scheduling");
        assertFalse(task.isCancelled(), "stockCheckTask should not be cancelled after scheduling");
    }

    @Test
    @DisplayName("startScheduledStockCheck: rescheduling replaces the existing task")
    void startScheduledStockCheck_rescheduled_replacesOldTask() throws Exception {
        SchedulerManager sm = SchedulerManager.getInstance();

        sm.startScheduledStockCheck(1, TimeUnit.HOURS);
        ScheduledFuture<?> first = stockCheckTask(sm);

        sm.startScheduledStockCheck(2, TimeUnit.HOURS);
        ScheduledFuture<?> second = stockCheckTask(sm);

        assertNotSame(first, second, "Rescheduling should replace the old task reference");
        assertTrue(first.isCancelled(), "Old task should be cancelled after rescheduling");
    }

    // =========================================================================
    // startScheduledStockCheck – invalid intervals
    // =========================================================================

    @Test
    @DisplayName("startScheduledStockCheck: zero interval is rejected – no task scheduled")
    void startScheduledStockCheck_zeroInterval_doesNotSchedule() throws Exception {
        SchedulerManager sm = SchedulerManager.getInstance();

        sm.startScheduledStockCheck(0, TimeUnit.MINUTES);

        assertNull(stockCheckTask(sm), "stockCheckTask should remain null for interval 0");
    }

    @Test
    @DisplayName("startScheduledStockCheck: negative interval is rejected – no task scheduled")
    void startScheduledStockCheck_negativeInterval_doesNotSchedule() throws Exception {
        SchedulerManager sm = SchedulerManager.getInstance();

        sm.startScheduledStockCheck(-5, TimeUnit.MINUTES);

        assertNull(stockCheckTask(sm), "stockCheckTask should remain null for negative interval");
    }

    @Test
    @DisplayName("startScheduledStockCheck: null TimeUnit is rejected – no task scheduled")
    void startScheduledStockCheck_nullUnit_doesNotSchedule() throws Exception {
        SchedulerManager sm = SchedulerManager.getInstance();

        sm.startScheduledStockCheck(5, null);

        assertNull(stockCheckTask(sm), "stockCheckTask should remain null for null TimeUnit");
    }

    // =========================================================================
    // startWarningDisplay
    // =========================================================================

    @Test
    @DisplayName("getNextWarningDisplayAtMillis: returns -1 before any warning display is scheduled")
    void getNextWarningDisplayAtMillis_beforeStart_returnsNegativeOne() {
        SchedulerManager sm = SchedulerManager.getInstance();
        assertEquals(-1L, sm.getNextWarningDisplayAtMillis());
    }

    @Test
    @DisplayName("startWarningDisplay: schedules task and sets nextWarningDisplayAtMillis to future")
    void startWarningDisplay_schedulesTaskAndSetsNextTime() throws Exception {
        SchedulerManager sm = SchedulerManager.getInstance();
        long before = System.currentTimeMillis();

        sm.startWarningDisplay(1, TimeUnit.HOURS);

        ScheduledFuture<?> task = warningDisplayTask(sm);
        long next = sm.getNextWarningDisplayAtMillis();
        assertNotNull(task, "warningDisplayTask should be non-null after scheduling");
        assertFalse(task.isCancelled(), "warningDisplayTask should not be cancelled after scheduling");
        assertTrue(next > before, "nextWarningDisplayAtMillis should be set to a future timestamp");
    }

    @Test
    @DisplayName("startWarningDisplay: zero interval is rejected – task and timestamp remain unset")
    void startWarningDisplay_zeroInterval_doesNotSchedule() throws Exception {
        SchedulerManager sm = SchedulerManager.getInstance();

        sm.startWarningDisplay(0, TimeUnit.MINUTES);

        assertNull(warningDisplayTask(sm), "warningDisplayTask should remain null for interval 0");
        assertEquals(-1L, sm.getNextWarningDisplayAtMillis());
    }

    @Test
    @DisplayName("startWarningDisplay: null TimeUnit is rejected – task and timestamp remain unset")
    void startWarningDisplay_nullUnit_doesNotSchedule() throws Exception {
        SchedulerManager sm = SchedulerManager.getInstance();

        sm.startWarningDisplay(1, null);

        assertNull(warningDisplayTask(sm), "warningDisplayTask should remain null for null TimeUnit");
        assertEquals(-1L, sm.getNextWarningDisplayAtMillis());
    }

    // =========================================================================
    // startAutoImportQrCodes
    // =========================================================================

    @Test
    @DisplayName("startAutoImportQrCodes: schedules task")
    void startAutoImportQrCodes_schedulesTask() throws Exception {
        SchedulerManager sm = SchedulerManager.getInstance();

        sm.startAutoImportQrCodes(1, TimeUnit.HOURS);

        ScheduledFuture<?> task = autoImportTask(sm);
        assertNotNull(task, "autoImportTask should be non-null after scheduling");
        assertFalse(task.isCancelled(), "autoImportTask should not be cancelled after scheduling");
    }

    @Test
    @DisplayName("startAutoImportQrCodes: zero interval is rejected – task remains null")
    void startAutoImportQrCodes_zeroInterval_doesNotSchedule() throws Exception {
        SchedulerManager sm = SchedulerManager.getInstance();

        sm.startAutoImportQrCodes(0, TimeUnit.MINUTES);

        assertNull(autoImportTask(sm), "autoImportTask should remain null for interval 0");
    }

    // =========================================================================
    // shutdown
    // =========================================================================

    @Test
    @DisplayName("shutdown: nulls out the executor and all task fields")
    void shutdown_clearsExecutorAndTasks() throws Exception {
        SchedulerManager sm = SchedulerManager.getInstance();
        sm.startScheduledStockCheck(1, TimeUnit.HOURS);
        sm.startWarningDisplay(1, TimeUnit.HOURS);

        sm.shutdown();

        assertNull(executor(sm), "Executor should be null after shutdown");
        assertNull(stockCheckTask(sm), "stockCheckTask should be null after shutdown");
        assertNull(warningDisplayTask(sm), "warningDisplayTask should be null after shutdown");
        assertEquals(-1L, sm.getNextWarningDisplayAtMillis());
    }

    @Test
    @DisplayName("shutdown: calling shutdown twice does not throw")
    void shutdown_calledTwice_isSafe() throws Exception {
        SchedulerManager sm = SchedulerManager.getInstance();
        sm.startScheduledStockCheck(1, TimeUnit.HOURS);

        assertDoesNotThrow(() -> {
            sm.shutdown();
            sm.shutdown();
        });
    }

    // =========================================================================
    // Restart after shutdown
    // =========================================================================

    @Test
    @DisplayName("startScheduledStockCheck: after shutdown, a new executor and task are created")
    void startAfterShutdown_recreatesExecutorAndTask() throws Exception {
        SchedulerManager sm = SchedulerManager.getInstance();
        sm.startScheduledStockCheck(1, TimeUnit.HOURS);
        sm.shutdown();

        sm.startScheduledStockCheck(1, TimeUnit.HOURS);

        ScheduledExecutorService exec = executor(sm);
        assertNotNull(exec, "Executor should be recreated after restart");
        assertFalse(exec.isShutdown(), "Recreated executor should not be shut down");
        assertNotNull(stockCheckTask(sm), "stockCheckTask should be non-null after restart");
    }
}
