package com.keystone.controller;

import com.keystone.dto.CreatePartRequest;
import com.keystone.dto.PartPageResponse;
import com.keystone.dto.PartResponse;
import com.keystone.dto.UpdatePartRequest;
import com.keystone.enums.PartStatus;
import com.keystone.service.PartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/parts")
@RequiredArgsConstructor
public class PartController {

    private final PartService partService;

    @GetMapping
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'VIEW_PART')")
    public ResponseEntity<PartPageResponse> getParts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name,asc") String sort,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) PartStatus status) {

        return ResponseEntity.ok(partService.getParts(page, size, sort, search, category, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'VIEW_PART')")
    public ResponseEntity<PartResponse> getPartById(@PathVariable Long id) {
        return ResponseEntity.ok(partService.getPartById(id));
    }

    @PostMapping
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'ADD_PART')")
    public ResponseEntity<PartResponse> createPart(
            @Valid @RequestBody CreatePartRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(partService.createPart(request, userDetails.getUsername()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'UPDATE_PART')")
    public ResponseEntity<PartResponse> updatePart(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePartRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(partService.updatePart(id, request, userDetails.getUsername()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'DELETE_PART')")
    public ResponseEntity<Void> deletePart(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        partService.deletePart(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
