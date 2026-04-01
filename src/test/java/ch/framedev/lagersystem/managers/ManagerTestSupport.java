package ch.framedev.lagersystem.managers;

import ch.framedev.lagersystem.main.Main;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

abstract class ManagerTestSupport {

    protected Path tempDir;
    protected DatabaseManager db;

    @BeforeEach
    void setUpManagerDatabase() throws IOException {
        resetAllManagerSingletons();
        tempDir = Files.createTempDirectory("vebo-manager-test-");
        db = new DatabaseManager(DatabaseManager.DatabaseType.H2, tempDir.toString(), "manager-testdb");
        db.initializeApplicationSchema();
        Main.databaseManager = db;
    }

    @AfterEach
    void tearDownManagerDatabase() throws IOException {
        resetAllManagerSingletons();
        if (db != null) {
            db.close();
        }
        Main.databaseManager = null;

        if (tempDir != null && Files.exists(tempDir)) {
            try (Stream<Path> walk = Files.walk(tempDir)) {
                walk.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(java.io.File::delete);
            }
        }
    }

    protected void resetAllManagerSingletons() {
        ArticleManager.resetInstance();
        ClientManager.resetInstance();
        resetSingleton(VendorManager.class);
        resetSingleton(UserManager.class);
        resetSingleton(WarningManager.class);
        resetSingleton(NotesManager.class);
        resetSingleton(DepartmentManager.class);
        resetSingleton(LogManager.class);
        resetSingleton(OrderManager.class);
    }

    private void resetSingleton(Class<?> type) {
        try {
            Field field = type.getDeclaredField("instance");
            field.setAccessible(true);
            field.set(null, null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Could not reset singleton for " + type.getSimpleName(), e);
        }
    }
}
