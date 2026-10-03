package com.keystone.service;

import com.keystone.dto.CreateUserRequest;
import com.keystone.dto.UpdateUserRequest;
import com.keystone.dto.UserPageResponse;
import com.keystone.dto.UserResponse;
import com.keystone.entity.User;
import com.keystone.enums.Role;
import com.keystone.exception.ApiException;
import com.keystone.exception.DuplicateResourceException;
import com.keystone.exception.ResourceNotFoundException;
import com.keystone.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private com.keystone.repository.CustomerRepository customerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User adminUser;
    private User managerUser;
    private User techUser;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(1L)
                .firstName("Admin")
                .lastName("Super")
                .userEmail("admin@keystone.com")
                .password("encoded_pass")
                .role(Role.ADMIN)
                .enabled(true)
                .build();

        managerUser = User.builder()
                .id(2L)
                .firstName("Manager")
                .lastName("Bob")
                .userEmail("manager@keystone.com")
                .password("encoded_pass")
                .role(Role.MANAGER)
                .enabled(true)
                .build();

        techUser = User.builder()
                .id(3L)
                .firstName("Tech")
                .lastName("Charlie")
                .userEmail("tech@keystone.com")
                .password("encoded_pass")
                .role(Role.TECHNICIAN)
                .enabled(true)
                .build();
    }

    @Test
    void getUsers_ShouldReturnPaginatedUsers() {
        when(userRepository.searchUsers(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(adminUser, managerUser)));

        UserPageResponse response = userService.getUsers(0, 10, "createdAt", null, null, null);

        assertNotNull(response);
        assertEquals(2, response.getContent().size());
        assertEquals(2, response.getTotalElements());
    }

    @Test
    void getUserById_WhenExists_ShouldReturnUserResponse() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        UserResponse response = userService.getUserById(1L);

        assertNotNull(response);
        assertEquals("admin@keystone.com", response.getUserEmail());
        assertEquals(Role.ADMIN, response.getRole());
    }

    @Test
    void getUserById_WhenNotFound_ShouldThrowException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(99L));
    }

    @Test
    void createUser_WithValidData_ShouldSaveUser() {
        CreateUserRequest request = CreateUserRequest.builder()
                .firstName("New")
                .lastName("User")
                .userEmail("new.user@keystone.com")
                .password("Password123!")
                .role(Role.TECHNICIAN)
                .build();

        when(userRepository.existsByUserEmail("new.user@keystone.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("hashed_pass");
        when(userRepository.save(any(User.class))).thenReturn(techUser);

        UserResponse response = userService.createUser(request);

        assertNotNull(response);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_WithDuplicateEmail_ShouldThrowException() {
        CreateUserRequest request = CreateUserRequest.builder()
                .firstName("New")
                .lastName("User")
                .userEmail("admin@keystone.com")
                .password("Password123!")
                .role(Role.TECHNICIAN)
                .build();

        when(userRepository.existsByUserEmail("admin@keystone.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> userService.createUser(request));
    }

    @Test
    void updateUser_ByManagerElevatingToAdmin_ShouldBeRejected() {
        UpdateUserRequest updateRequest = UpdateUserRequest.builder()
                .firstName("Tech")
                .lastName("Charlie")
                .role(Role.ADMIN) // Attempting privilege escalation to ADMIN
                .enabled(true)
                .build();

        when(userRepository.findById(3L)).thenReturn(Optional.of(techUser));
        when(userRepository.findByUserEmail("manager@keystone.com")).thenReturn(Optional.of(managerUser));

        assertThrows(ApiException.class, () -> userService.updateUser(3L, updateRequest, "manager@keystone.com"));
    }

    @Test
    void updateUser_AdminDemotingThemselves_ShouldBeRejected() {
        UpdateUserRequest updateRequest = UpdateUserRequest.builder()
                .firstName("Admin")
                .lastName("Super")
                .role(Role.TECHNICIAN) // Admin trying to demote themselves
                .enabled(true)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(userRepository.findByUserEmail("admin@keystone.com")).thenReturn(Optional.of(adminUser));

        assertThrows(ApiException.class, () -> userService.updateUser(1L, updateRequest, "admin@keystone.com"));
    }

    @Test
    void deleteUser_SelfDeletion_ShouldBeRejected() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(userRepository.findByUserEmail("admin@keystone.com")).thenReturn(Optional.of(adminUser));

        assertThrows(ApiException.class, () -> userService.deleteUser(1L, "admin@keystone.com"));
    }
}
