package com.keystone.service;

import com.keystone.dto.CreateUserRequest;
import com.keystone.dto.UpdateUserRequest;
import com.keystone.dto.UserPageResponse;
import com.keystone.dto.UserResponse;
import com.keystone.enums.Role;

public interface UserService {

    UserPageResponse getUsers(int page, int size, String sort, String search, Role role, Boolean enabled);

    UserResponse getUserById(Long id);

    UserResponse createUser(CreateUserRequest request);

    UserResponse updateUser(Long id, UpdateUserRequest request, String currentUsername);

    UserResponse updateUserStatus(Long id, Boolean enabled, String currentUsername);

    void deleteUser(Long id, String currentUsername);
}
