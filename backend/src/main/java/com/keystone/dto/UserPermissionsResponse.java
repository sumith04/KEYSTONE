package com.keystone.dto;

import com.keystone.enums.Permission;
import com.keystone.enums.Role;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPermissionsResponse {

    private Role role;
    private Set<Permission> permissions;
}
