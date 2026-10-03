package com.keystone.config;

import com.keystone.enums.Permission;
import com.keystone.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RolePermissionMapperTest {

    private RolePermissionMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new RolePermissionMapper();
    }

    @Test
    void admin_ShouldHaveAllPermissions() {
        Set<Permission> adminPermissions = mapper.getPermissionsForRole(Role.ADMIN);
        assertEquals(Permission.values().length, adminPermissions.size());
        assertTrue(adminPermissions.contains(Permission.DELETE_USER));
        assertTrue(adminPermissions.contains(Permission.CREATE_USER));
        assertTrue(adminPermissions.contains(Permission.SEND_NOTIFICATION));
    }

    @Test
    void manager_ShouldHaveDefinedPermissions_AndNotDeleteUser() {
        Set<Permission> managerPermissions = mapper.getPermissionsForRole(Role.MANAGER);
        assertTrue(managerPermissions.contains(Permission.CREATE_USER));
        assertTrue(managerPermissions.contains(Permission.UPDATE_USER));
        assertTrue(managerPermissions.contains(Permission.VIEW_USER));
        assertFalse(managerPermissions.contains(Permission.DELETE_USER)); // Must NOT have DELETE_USER
        assertTrue(managerPermissions.contains(Permission.VIEW_DASHBOARD));
        assertTrue(managerPermissions.contains(Permission.VIEW_REPORT));
    }

    @Test
    void dispatcher_ShouldHaveWorkOrderAndSitePermissions_AndNoUserManagement() {
        Set<Permission> dispatcherPermissions = mapper.getPermissionsForRole(Role.DISPATCHER);
        assertTrue(dispatcherPermissions.contains(Permission.VIEW_CUSTOMER));
        assertTrue(dispatcherPermissions.contains(Permission.VIEW_SITE));
        assertTrue(dispatcherPermissions.contains(Permission.CREATE_WORK_ORDER));
        assertTrue(dispatcherPermissions.contains(Permission.ASSIGN_WORK_ORDER));
        assertFalse(dispatcherPermissions.contains(Permission.CREATE_USER));
        assertFalse(dispatcherPermissions.contains(Permission.UPDATE_USER));
        assertFalse(dispatcherPermissions.contains(Permission.DELETE_USER));
    }

    @Test
    void technician_ShouldHaveWorkExecutionPermissions_AndNoUserManagement() {
        Set<Permission> techPermissions = mapper.getPermissionsForRole(Role.TECHNICIAN);
        assertTrue(techPermissions.contains(Permission.START_WORK));
        assertTrue(techPermissions.contains(Permission.COMPLETE_WORK));
        assertTrue(techPermissions.contains(Permission.ADD_PART));
        assertTrue(techPermissions.contains(Permission.ADD_TIME_LOG));
        assertFalse(techPermissions.contains(Permission.CREATE_USER));
        assertFalse(techPermissions.contains(Permission.UPDATE_USER));
        assertFalse(techPermissions.contains(Permission.DELETE_USER));
    }

    @Test
    void customer_ShouldHaveSelfServicePermissionsOnly() {
        Set<Permission> customerPermissions = mapper.getPermissionsForRole(Role.CUSTOMER);
        assertTrue(customerPermissions.contains(Permission.REQUEST_RAISE));
        assertTrue(customerPermissions.contains(Permission.VIEW_OWN_REQUEST));
        assertEquals(4, customerPermissions.size()); // LOGIN, LOGOUT, REQUEST_RAISE, VIEW_OWN_REQUEST
        assertFalse(customerPermissions.contains(Permission.VIEW_USER));
        assertFalse(customerPermissions.contains(Permission.CREATE_USER));
    }
}
