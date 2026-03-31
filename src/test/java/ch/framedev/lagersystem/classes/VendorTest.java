package ch.framedev.lagersystem.classes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VendorTest {

    @Test
    @DisplayName("full constructor: stores all vendor fields")
    void fullConstructor_storesAllFields() {
        Vendor vendor = new Vendor(
                "Acme",
                "Anna",
                "0123",
                "anna@example.com",
                "Musterstrasse 1",
                List.of("1001", "1002"),
                250.0);

        assertEquals("Acme", vendor.getName());
        assertEquals("Anna", vendor.getContactPerson());
        assertEquals("0123", vendor.getPhoneNumber());
        assertEquals("anna@example.com", vendor.getEmail());
        assertEquals("Musterstrasse 1", vendor.getAddress());
        assertEquals(List.of("1001", "1002"), vendor.getSuppliedArticles());
        assertEquals(250.0, vendor.getMinOrderValue(), 1e-9);
    }

    @Test
    @DisplayName("five-argument constructor: initializes empty supplied articles list")
    void fiveArgumentConstructor_initializesEmptySuppliedArticlesList() {
        Vendor vendor = new Vendor("Acme", "Anna", "0123", "anna@example.com", "Musterstrasse 1");

        assertNotNull(vendor.getSuppliedArticles());
        assertTrue(vendor.getSuppliedArticles().isEmpty());
    }

    @Test
    @DisplayName("setters: update vendor fields")
    void setters_updateFields() {
        Vendor vendor = new Vendor("Acme", "Anna", "0123", "anna@example.com", "Musterstrasse 1");

        vendor.setName("Beta");
        vendor.setContactPerson("Bernd");
        vendor.setPhoneNumber("9999");
        vendor.setEmail("bernd@example.com");
        vendor.setAddress("Neue Adresse 2");
        vendor.setSuppliedArticles(new ArrayList<>(List.of("2201")));
        vendor.setMinOrderValue(123.45);

        assertEquals("Beta", vendor.getName());
        assertEquals("Bernd", vendor.getContactPerson());
        assertEquals("9999", vendor.getPhoneNumber());
        assertEquals("bernd@example.com", vendor.getEmail());
        assertEquals("Neue Adresse 2", vendor.getAddress());
        assertEquals(List.of("2201"), vendor.getSuppliedArticles());
        assertEquals(123.45, vendor.getMinOrderValue(), 1e-9);
    }

    @Test
    @DisplayName("getQRCodeData: contains all labeled fields")
    void getQRCodeData_containsAllFields() {
        Vendor vendor = new Vendor(
                "Acme",
                "Anna",
                "0123",
                "anna@example.com",
                "Musterstrasse 1",
                List.of("1001", "1002"),
                250.0);

        String qrData = vendor.getQRCodeData();

        assertEquals(
                "name:Acme;contactPerson:Anna;phoneNumber:0123;email:anna@example.com;address:Musterstrasse 1;suppliedArticles:1001,1002;minOrderValue:250.0",
                qrData);
    }
}
