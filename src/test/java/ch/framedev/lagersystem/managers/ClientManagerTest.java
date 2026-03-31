package ch.framedev.lagersystem.managers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientManagerTest extends ManagerTestSupport {

    @Test
    @DisplayName("insert/get/update/delete: client CRUD works")
    void clientCrud_works() {
        ClientManager manager = ClientManager.getInstance();

        assertTrue(manager.insertClient("Max Muster", "Einkauf"));
        assertTrue(manager.existsClient("Max Muster"));
        assertEquals("Einkauf", manager.getDepartmentByName("Max Muster"));

        assertTrue(manager.updateClient("Max Muster", "Verkauf"));
        assertEquals("Verkauf", manager.getDepartmentByName("Max Muster"));

        assertTrue(manager.deleteClient("Max Muster"));
        assertFalse(manager.existsClient("Max Muster"));
        assertNull(manager.getDepartmentByName("Max Muster"));
    }

    @Test
    @DisplayName("insertClient: blank name is rejected")
    void insertClient_blankName_rejected() {
        ClientManager manager = ClientManager.getInstance();

        assertFalse(manager.insertClient("   ", "Einkauf"));
    }

    @Test
    @DisplayName("getAllClients: returns inserted clients with department values")
    void getAllClients_returnsInsertedClients() {
        ClientManager manager = ClientManager.getInstance();
        manager.insertClient("Alice Example", "IT");
        manager.insertClient("Bob Example", "HR");

        List<Map<String, String>> clients = manager.getAllClients();

        assertEquals(2, clients.size());
        assertTrue(clients.stream().anyMatch(client ->
                "Alice Example".equals(client.get("firstLastName")) && "IT".equals(client.get("department"))));
        assertTrue(clients.stream().anyMatch(client ->
                "Bob Example".equals(client.get("firstLastName")) && "HR".equals(client.get("department"))));
    }
}
