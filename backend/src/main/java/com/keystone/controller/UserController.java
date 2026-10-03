package com.keystone.controller;

import com.keystone.dto.CreateUserRequest;
import com.keystone.dto.UpdateUserRequest;
import com.keystone.dto.UserPageResponse;
import com.keystone.dto.UserResponse;
import com.keystone.enums.Role;
import com.keystone.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'VIEW_USER')")
    public ResponseEntity<UserPageResponse> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean enabled) {
        
        UserPageResponse response = userService.getUsers(page, size, sort, search, role, enabled);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'VIEW_USER')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'CREATE_USER')")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'UPDATE_USER')")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UserResponse response = userService.updateUser(id, request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'UPDATE_USER')")
    public ResponseEntity<UserResponse> updateUserStatus(
            @PathVariable Long id,
            @RequestParam Boolean enabled,
            @AuthenticationPrincipal UserDetails userDetails) {

        UserResponse response = userService.updateUserStatus(id, enabled, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'DELETE_USER')")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        userService.deleteUser(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
