package ch.framedev.lagersystem.managers;

import ch.framedev.lagersystem.classes.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserManagerTest extends ManagerTestSupport {

    @Test
    @DisplayName("insert/get/update/delete: user CRUD works")
    void userCrud_works() {
        UserManager manager = UserManager.getInstance();
        User user = new User("anna", List.of("ORD-1"));

        assertTrue(manager.insertUser(user));
        assertTrue(manager.existsUser("anna"));
        assertEquals(List.of("ORD-1"), manager.getUserByName("anna").getOrders());

        assertTrue(manager.updateUser(new User("anna", List.of("ORD-2", "ORD-3"))));
        assertEquals(List.of("ORD-2", "ORD-3"), manager.getUserByName("anna").getOrders());

        assertTrue(manager.deleteUser("anna"));
        assertFalse(manager.existsUser("anna"));
        assertNull(manager.getUserByName("anna"));
    }

    @Test
    @DisplayName("updateUser(name, orders): updates existing order list")
    void updateUserByName_updatesOrderList() {
        UserManager manager = UserManager.getInstance();
        manager.insertUser(new User("bernd", List.of("ORD-1")));

        assertTrue(manager.updateUser("bernd", List.of("ORD-9")));
        assertEquals(List.of("ORD-9"), manager.getUserByName("bernd").getOrders());
    }

    @Test
    @DisplayName("getAllUsernames: returns inserted usernames")
    void getAllUsernames_returnsInsertedUsernames() {
        UserManager manager = UserManager.getInstance();
        manager.insertUser(new User("alice", List.of()));
        manager.insertUser(new User("bob", List.of()));

        List<String> usernames = manager.getAllUsernames();

        assertEquals(2, usernames.size());
        assertTrue(usernames.contains("alice"));
        assertTrue(usernames.contains("bob"));
    }
}
