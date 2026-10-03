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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public UserPageResponse getUsers(int page, int size, String sort, String search, Role role, Boolean enabled) {
        String sortProperty = (sort != null && !sort.isBlank()) ? sort.trim() : "createdAt";
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortProperty));

        String searchTerm = (search != null && !search.isBlank()) ? search.trim() : null;

        Page<User> userPage = userRepository.searchUsers(searchTerm, role, enabled, pageable);

        List<UserResponse> content = userPage.getContent()
                .stream()
                .map(UserResponse::fromEntity)
                .toList();

        return UserPageResponse.builder()
                .content(content)
                .pageNumber(userPage.getNumber())
                .pageSize(userPage.getSize())
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .isLast(userPage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return UserResponse.fromEntity(user);
    }

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        String normalizedEmail = request.getUserEmail().trim().toLowerCase();

        if (userRepository.existsByUserEmail(normalizedEmail)) {
            throw new DuplicateResourceException("An account with email address " + normalizedEmail + " already exists.");
        }

        User user = User.builder()
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .userEmail(normalizedEmail)
                .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);
        return UserResponse.fromEntity(savedUser);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request, String currentUsername) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        User currentUser = userRepository.findByUserEmail(currentUsername.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Current authenticated user not found"));

        // Privilege Escalation Prevention: Non-ADMIN cannot assign ADMIN role
        if (request.getRole() == Role.ADMIN && currentUser.getRole() != Role.ADMIN) {
            throw new ApiException("Only ADMIN users can assign the ADMIN role.", HttpStatus.FORBIDDEN);
        }

        // ADMIN Self-Demotion Prevention: ADMIN cannot change their own role away from ADMIN
        if (Objects.equals(currentUser.getId(), user.getId()) && currentUser.getRole() == Role.ADMIN && request.getRole() != Role.ADMIN) {
            throw new ApiException("ADMIN users cannot demote their own role.", HttpStatus.BAD_REQUEST);
        }

        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
        user.setRole(request.getRole());
        user.setEnabled(request.getEnabled());

        User updatedUser = userRepository.save(user);
        return UserResponse.fromEntity(updatedUser);
    }

    @Override
    @Transactional
    public UserResponse updateUserStatus(Long id, Boolean enabled, String currentUsername) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        User currentUser = userRepository.findByUserEmail(currentUsername.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Current authenticated user not found"));

        if (Objects.equals(currentUser.getId(), user.getId()) && !enabled) {
            throw new ApiException("Users cannot disable their own account.", HttpStatus.BAD_REQUEST);
        }

        user.setEnabled(enabled);
        User updatedUser = userRepository.save(user);
        return UserResponse.fromEntity(updatedUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long id, String currentUsername) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        User currentUser = userRepository.findByUserEmail(currentUsername.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Current authenticated user not found"));

        if (Objects.equals(currentUser.getId(), user.getId())) {
            throw new ApiException("Users cannot delete their own account.", HttpStatus.BAD_REQUEST);
        }

        userRepository.delete(user);
    }
}
