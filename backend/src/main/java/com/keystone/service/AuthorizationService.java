package com.keystone.service;

import com.keystone.config.RolePermissionMapper;
import com.keystone.entity.User;
import com.keystone.enums.Permission;
import com.keystone.enums.Role;
import com.keystone.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

@Service("authorizationService")
@RequiredArgsConstructor
public class AuthorizationService {

    private final RolePermissionMapper rolePermissionMapper;
    private final UserRepository userRepository;

    public boolean hasPermission(Role role, Permission permission) {
        return rolePermissionMapper.hasPermission(role, permission);
    }

    public boolean hasPermission(Authentication authentication, String permissionName) {
        if (authentication == null || !authentication.isAuthenticated() || permissionName == null) {
            return false;
        }

        try {
            Permission permission = Permission.valueOf(permissionName.trim().toUpperCase());
            return hasPermission(authentication, permission);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public boolean hasPermission(Authentication authentication, Permission permission) {
        if (authentication == null || !authentication.isAuthenticated() || permission == null) {
            return false;
        }

        Role role = extractRoleFromAuthentication(authentication);
        if (role == null) {
            return false;
        }

        return rolePermissionMapper.hasPermission(role, permission);
    }

    public Set<Permission> getPermissionsForUser(String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            return Set.of();
        }
        Optional<User> userOptional = userRepository.findByUserEmail(userEmail.trim().toLowerCase());
        if (userOptional.isEmpty()) {
            return Set.of();
        }
        return rolePermissionMapper.getPermissionsForRole(userOptional.get().getRole());
    }

    private Role extractRoleFromAuthentication(Authentication authentication) {
        if (authentication.getAuthorities() != null) {
            for (GrantedAuthority authority : authentication.getAuthorities()) {
                String authorityName = authority.getAuthority();
                if (authorityName.startsWith("ROLE_")) {
                    String roleStr = authorityName.substring(5);
                    try {
                        return Role.valueOf(roleStr);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            Optional<User> userOptional = userRepository.findByUserEmail(userDetails.getUsername().toLowerCase());
            if (userOptional.isPresent()) {
                return userOptional.get().getRole();
            }
        } else if (principal instanceof String email) {
            Optional<User> userOptional = userRepository.findByUserEmail(email.toLowerCase());
            if (userOptional.isPresent()) {
                return userOptional.get().getRole();
            }
        }

        return null;
    }
}
