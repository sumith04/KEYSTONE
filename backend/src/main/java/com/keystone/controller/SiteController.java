package com.keystone.controller;

import com.keystone.dto.CreateSiteRequest;
import com.keystone.dto.SitePageResponse;
import com.keystone.dto.SiteResponse;
import com.keystone.dto.UpdateSiteRequest;
import com.keystone.enums.SiteStatus;
import com.keystone.service.SiteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sites")
@RequiredArgsConstructor
public class SiteController {

    private final SiteService siteService;

    @GetMapping
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'VIEW_SITE')")
    public ResponseEntity<SitePageResponse> getSites(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) SiteStatus status,
            @RequestParam(required = false) Long customerId) {

        SitePageResponse response = siteService.getSites(page, size, sort, search, status, customerId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'VIEW_SITE')")
    public ResponseEntity<SiteResponse> getSiteById(@PathVariable Long id) {
        return ResponseEntity.ok(siteService.getSiteById(id));
    }

    @PostMapping
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'CREATE_SITE')")
    public ResponseEntity<SiteResponse> createSite(@Valid @RequestBody CreateSiteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(siteService.createSite(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'UPDATE_SITE')")
    public ResponseEntity<SiteResponse> updateSite(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSiteRequest request) {
        return ResponseEntity.ok(siteService.updateSite(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'UPDATE_SITE')")
    public ResponseEntity<SiteResponse> updateSiteStatus(
            @PathVariable Long id,
            @RequestParam SiteStatus status) {
        return ResponseEntity.ok(siteService.updateSiteStatus(id, status));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'DELETE_SITE')")
    public ResponseEntity<Void> deleteSite(@PathVariable Long id) {
        siteService.deleteSite(id);
        return ResponseEntity.noContent().build();
    }
}
