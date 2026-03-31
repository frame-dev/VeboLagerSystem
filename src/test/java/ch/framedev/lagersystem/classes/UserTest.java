package ch.framedev.lagersystem.classes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTest {

    @Test
    @DisplayName("constructor: stores name and orders")
    void constructor_storesFields() {
        User user = new User("Max Muster", List.of("ORD-1", "ORD-2"));

        assertEquals("Max Muster", user.getName());
        assertEquals(List.of("ORD-1", "ORD-2"), user.getOrders());
    }

    @Test
    @DisplayName("getOrders: returns empty list when internal orders are null")
    void getOrders_returnsEmptyListWhenNull() {
        User user = new User("Max Muster", null);

        List<String> orders = user.getOrders();

        assertNotNull(orders);
        assertTrue(orders.isEmpty());
    }

    @Test
    @DisplayName("setOrders: replaces order list")
    void setOrders_replacesOrderList() {
        User user = new User("Max Muster", new ArrayList<>());

        user.setOrders(List.of("ORD-9"));

        assertEquals(List.of("ORD-9"), user.getOrders());
    }

    @Test
    @DisplayName("getQRCodeData: joins orders as comma separated list")
    void getQRCodeData_joinsOrders() {
        User user = new User("Max Muster", List.of("ORD-1", "ORD-2"));

        assertEquals("name:Max Muster;orders:ORD-1,ORD-2", user.getQRCodeData());
    }

    @Test
    @DisplayName("getQRCodeData: handles empty orders")
    void getQRCodeData_handlesEmptyOrders() {
        User user = new User("Max Muster", List.of());

        assertEquals("name:Max Muster;orders:", user.getQRCodeData());
    }
}
