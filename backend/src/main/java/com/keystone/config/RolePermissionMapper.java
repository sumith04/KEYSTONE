package com.keystone.config;

import com.keystone.enums.Permission;
import com.keystone.enums.Role;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class RolePermissionMapper {

    private final Map<Role, Set<Permission>> rolePermissionsMap = new EnumMap<>(Role.class);

    public RolePermissionMapper() {
        initializeMappings();
    }

    private void initializeMappings() {
        // ADMIN has all permissions
        Set<Permission> adminPermissions = EnumSet.allOf(Permission.class);
        rolePermissionsMap.put(Role.ADMIN, Collections.unmodifiableSet(adminPermissions));

        // MANAGER permissions
        Set<Permission> managerPermissions = EnumSet.of(
                Permission.LOGIN,
                Permission.LOGOUT,
                Permission.CREATE_USER,
                Permission.UPDATE_USER,
                Permission.VIEW_USER,
                Permission.CREATE_CUSTOMER,
                Permission.UPDATE_CUSTOMER,
                Permission.VIEW_CUSTOMER,
                Permission.DELETE_CUSTOMER,
                Permission.CREATE_SITE,
                Permission.UPDATE_SITE,
                Permission.VIEW_SITE,
                Permission.DELETE_SITE,
                Permission.CREATE_WORK_ORDER,
                Permission.UPDATE_WORK_ORDER,
                Permission.VIEW_WORK_ORDER,
                Permission.DELETE_WORK_ORDER,
                Permission.ASSIGN_WORK_ORDER,
                Permission.CANCEL_WORK_ORDER,
                Permission.CLOSE_WORK_ORDER,
                Permission.VIEW_PART,
                Permission.VIEW_TIME_LOGS,
                Permission.VIEW_DASHBOARD,
                Permission.VIEW_REPORT,
                Permission.SEND_NOTIFICATION
        );
        rolePermissionsMap.put(Role.MANAGER, Collections.unmodifiableSet(managerPermissions));

        // DISPATCHER permissions
        Set<Permission> dispatcherPermissions = EnumSet.of(
                Permission.LOGIN,
                Permission.LOGOUT,
                Permission.VIEW_CUSTOMER,
                Permission.VIEW_SITE,
                Permission.CREATE_WORK_ORDER,
                Permission.UPDATE_WORK_ORDER,
                Permission.VIEW_WORK_ORDER,
                Permission.ASSIGN_WORK_ORDER,
                Permission.VIEW_DASHBOARD
        );
        rolePermissionsMap.put(Role.DISPATCHER, Collections.unmodifiableSet(dispatcherPermissions));

        // TECHNICIAN permissions
        Set<Permission> technicianPermissions = EnumSet.of(
                Permission.LOGIN,
                Permission.LOGOUT,
                Permission.VIEW_WORK_ORDER,
                Permission.VIEW_SITE,
                Permission.START_WORK,
                Permission.HOLD_WORK,
                Permission.RESUME_WORK,
                Permission.COMPLETE_WORK,
                Permission.ADD_PART,
                Permission.UPDATE_PART,
                Permission.DELETE_PART,
                Permission.VIEW_PART,
                Permission.USE_PARTS,
                Permission.ADD_TIME_LOG,
                Permission.VIEW_TIME_LOGS
        );
        rolePermissionsMap.put(Role.TECHNICIAN, Collections.unmodifiableSet(technicianPermissions));

        // CUSTOMER permissions
        Set<Permission> customerPermissions = EnumSet.of(
                Permission.LOGIN,
                Permission.LOGOUT,
                Permission.REQUEST_RAISE,
                Permission.VIEW_OWN_REQUEST
        );
        rolePermissionsMap.put(Role.CUSTOMER, Collections.unmodifiableSet(customerPermissions));
    }

    public Set<Permission> getPermissionsForRole(Role role) {
        if (role == null) {
            return Collections.emptySet();
        }
        return rolePermissionsMap.getOrDefault(role, Collections.emptySet());
    }

    public boolean hasPermission(Role role, Permission permission) {
        if (role == null || permission == null) {
            return false;
        }
        Set<Permission> permissions = getPermissionsForRole(role);
        return permissions.contains(permission);
    }
}
