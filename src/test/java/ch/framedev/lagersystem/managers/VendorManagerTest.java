package ch.framedev.lagersystem.managers;

import ch.framedev.lagersystem.classes.Vendor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VendorManagerTest extends ManagerTestSupport {

    @Test
    @DisplayName("insert/get/update/delete: vendor CRUD works")
    void vendorCrud_works() {
        VendorManager manager = VendorManager.getInstance();
        Vendor vendor = new Vendor("Acme", "Anna", "0123", "anna@example.com", "Street 1", new ArrayList<>(List.of("1001")), 50.0);

        assertTrue(manager.insertVendor(vendor));
        assertTrue(manager.existsVendor("Acme"));
        assertEquals(List.of("1001"), manager.getVendorByName("Acme").getSuppliedArticles());

        vendor.setPhoneNumber("9999");
        vendor.setSuppliedArticles(new ArrayList<>(List.of("1001", "1002")));
        assertTrue(manager.updateVendor(vendor));
        assertEquals("9999", manager.getVendorByName("Acme").getPhoneNumber());
        assertEquals(List.of("1001", "1002"), manager.getVendorByName("Acme").getSuppliedArticles());

        assertTrue(manager.deleteVendor("Acme"));
        assertFalse(manager.existsVendor("Acme"));
        assertNull(manager.getVendorByName("Acme"));
    }

    @Test
    @DisplayName("updateVendor(columns,data): updates selected columns dynamically")
    void updateVendorDynamic_updatesSelectedColumns() {
        VendorManager manager = VendorManager.getInstance();
        manager.insertVendor(new Vendor("Beta", "Ben", "111", "ben@example.com", "Old Street", new ArrayList<>(List.of("2001")), 10.0));

        boolean updated = manager.updateVendor("Beta",
                new String[]{"contactPerson", "minOrderValue"},
                new Object[]{"Beatrice", 99.5});

        Vendor vendor = manager.getVendorByName("Beta");
        assertTrue(updated);
        assertEquals("Beatrice", vendor.getContactPerson());
        assertEquals(99.5, vendor.getMinOrderValue(), 1e-9);
    }

    @Test
    @DisplayName("getVendors: returns all inserted vendors")
    void getVendors_returnsAllInsertedVendors() {
        VendorManager manager = VendorManager.getInstance();
        manager.insertVendor(new Vendor("Vendor1", "A", "1", "a@example.com", "Addr1", new ArrayList<>(List.of("1001")), 10.0));
        manager.insertVendor(new Vendor("Vendor2", "B", "2", "b@example.com", "Addr2", new ArrayList<>(List.of("1002")), 20.0));

        List<Vendor> vendors = manager.getVendors();

        assertEquals(2, vendors.size());
        assertTrue(vendors.stream().anyMatch(v -> "Vendor1".equals(v.getName())));
        assertTrue(vendors.stream().anyMatch(v -> "Vendor2".equals(v.getName())));
    }
}
