package com.keystone.controller;

import com.keystone.dto.CreateSlaPolicyRequest;
import com.keystone.dto.SlaPolicyPageResponse;
import com.keystone.dto.SlaPolicyResponse;
import com.keystone.dto.UpdateSlaPolicyRequest;
import com.keystone.enums.WorkOrderPriority;
import com.keystone.service.SlaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sla-policies")
@RequiredArgsConstructor
public class SlaPolicyController {

    private final SlaService slaService;

    @GetMapping
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'VIEW_SLA')")
    public ResponseEntity<SlaPolicyPageResponse> getPolicies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name,asc") String sort,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) WorkOrderPriority priority,
            @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(slaService.getPolicies(page, size, sort, search, priority, active));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'VIEW_SLA')")
    public ResponseEntity<SlaPolicyResponse> getPolicyById(@PathVariable Long id) {
        return ResponseEntity.ok(slaService.getPolicyById(id));
    }

    @PostMapping
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'CREATE_SLA')")
    public ResponseEntity<SlaPolicyResponse> createPolicy(@Valid @RequestBody CreateSlaPolicyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(slaService.createPolicy(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'UPDATE_SLA')")
    public ResponseEntity<SlaPolicyResponse> updatePolicy(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSlaPolicyRequest request) {
        return ResponseEntity.ok(slaService.updatePolicy(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authorizationService.hasPermission(authentication, 'DELETE_SLA')")
    public ResponseEntity<Void> deletePolicy(@PathVariable Long id) {
        slaService.deletePolicy(id);
        return ResponseEntity.noContent().build();
    }
}
