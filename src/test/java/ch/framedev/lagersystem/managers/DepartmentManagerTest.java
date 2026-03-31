package ch.framedev.lagersystem.managers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DepartmentManagerTest extends ManagerTestSupport {

    @Test
    @DisplayName("insert/get/update/delete: department CRUD works")
    void departmentCrud_works() {
        DepartmentManager manager = DepartmentManager.getInstance();

        assertTrue(manager.insertDepartment("Lager", "1000"));
        assertTrue(manager.existsDepartment("Lager"));
        assertEquals("1000", manager.getDepartment("Lager").get("kontoNumber"));

        assertTrue(manager.updateDepartment("Lager", "2000"));
        assertEquals("2000", manager.getDepartment("Lager").get("kontoNumber"));

        assertTrue(manager.deleteDepartment("Lager"));
        assertFalse(manager.existsDepartment("Lager"));
        assertNull(manager.getDepartment("Lager"));
    }

    @Test
    @DisplayName("insertDepartment: duplicate department is rejected")
    void insertDepartment_duplicate_rejected() {
        DepartmentManager manager = DepartmentManager.getInstance();

        assertTrue(manager.insertDepartment("IT", "1111"));
        assertFalse(manager.insertDepartment("IT", "2222"));
    }

    @Test
    @DisplayName("getAllDepartments: returns all inserted departments")
    void getAllDepartments_returnsAllInsertedDepartments() {
        DepartmentManager manager = DepartmentManager.getInstance();
        manager.insertDepartment("IT", "1111");
        manager.insertDepartment("HR", "2222");

        List<Map<String, Object>> departments = manager.getAllDepartments();

        assertEquals(2, departments.size());
        assertTrue(departments.stream().anyMatch(entry ->
                "IT".equals(entry.get("department")) && "1111".equals(entry.get("kontoNumber"))));
        assertTrue(departments.stream().anyMatch(entry ->
                "HR".equals(entry.get("department")) && "2222".equals(entry.get("kontoNumber"))));
    }
}
