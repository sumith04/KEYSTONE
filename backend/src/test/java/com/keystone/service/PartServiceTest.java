package com.keystone.service;

import com.keystone.dto.CreatePartRequest;
import com.keystone.dto.PartPageResponse;
import com.keystone.dto.PartResponse;
import com.keystone.dto.UpdatePartRequest;
import com.keystone.entity.Part;
import com.keystone.entity.User;
import com.keystone.enums.PartStatus;
import com.keystone.enums.Role;
import com.keystone.exception.ApiException;
import com.keystone.exception.DuplicateResourceException;
import com.keystone.exception.ResourceNotFoundException;
import com.keystone.repository.PartRepository;
import com.keystone.repository.WorkOrderPartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PartServiceTest {

    private static final String ADMIN_EMAIL = "admin@keystone.com";
    private static final String TECH_EMAIL = "terry@keystone.com";

    @Mock
    private PartRepository partRepository;

    @Mock
    private WorkOrderPartRepository workOrderPartRepository;

    @Mock
    private WorkOrderAccessGuard workOrderAccessGuard;

    @InjectMocks
    private PartServiceImpl partService;

    private User admin;
    private User technician;
    private Part filter;

    @BeforeEach
    void setUp() {
        admin = User.builder()
                .id(1L)
                .firstName("Avery")
                .lastName("Admin")
                .userEmail(ADMIN_EMAIL)
                .role(Role.ADMIN)
                .enabled(true)
                .build();

        technician = User.builder()
                .id(200L)
                .firstName("Terry")
                .lastName("Tech")
                .userEmail(TECH_EMAIL)
                .role(Role.TECHNICIAN)
                .enabled(true)
                .build();

        filter = Part.builder()
                .id(10L)
                .partNumber("FLT-100")
                .name("HVAC Filter")
                .description("Standard filter")
                .category("Filters")
                .unitOfMeasure("EA")
                .unitCost(new BigDecimal("12.50"))
                .quantityInStock(20)
                .reorderLevel(5)
                .status(PartStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createPart_WhenValid_ShouldNormalizeAndSave() {
        CreatePartRequest request = validCreateRequest();
        when(workOrderAccessGuard.requireCurrentUser(ADMIN_EMAIL)).thenReturn(admin);
        when(partRepository.existsByPartNumber("FLT-100")).thenReturn(false);
        when(partRepository.save(any(Part.class))).thenReturn(filter);

        PartResponse response = partService.createPart(request, ADMIN_EMAIL);

        ArgumentCaptor<Part> captor = ArgumentCaptor.forClass(Part.class);
        verify(partRepository).save(captor.capture());
        assertEquals("FLT-100", captor.getValue().getPartNumber());
        assertEquals(new BigDecimal("12.50"), captor.getValue().getUnitCost());
        assertEquals("FLT-100", response.getPartNumber());
    }

    @Test
    void createPart_WhenDuplicatePartNumber_ShouldConflict() {
        when(workOrderAccessGuard.requireCurrentUser(ADMIN_EMAIL)).thenReturn(admin);
        when(partRepository.existsByPartNumber("FLT-100")).thenReturn(true);

        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> partService.createPart(validCreateRequest(), ADMIN_EMAIL)
        );
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(partRepository, never()).save(any());
    }

    @Test
    void createPart_WhenTechnician_ShouldBeForbidden() {
        when(workOrderAccessGuard.requireCurrentUser(TECH_EMAIL)).thenReturn(technician);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> partService.createPart(validCreateRequest(), TECH_EMAIL)
        );
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        verify(partRepository, never()).save(any());
    }

    @Test
    void createPart_WhenNegativeQuantity_ShouldReject() {
        CreatePartRequest request = validCreateRequest();
        request.setQuantityInStock(-1);
        when(workOrderAccessGuard.requireCurrentUser(ADMIN_EMAIL)).thenReturn(admin);
        when(partRepository.existsByPartNumber("FLT-100")).thenReturn(false);

        ApiException exception = assertThrows(ApiException.class, () -> partService.createPart(request, ADMIN_EMAIL));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void getParts_ShouldUseRepositoryFilters() {
        when(partRepository.searchParts(eq("filter"), eq("Filters"), eq(PartStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(filter)));

        PartPageResponse response = partService.getParts(0, 10, "name,asc", "filter", "Filters", PartStatus.ACTIVE);

        assertEquals(1, response.getContent().size());
        assertEquals("FLT-100", response.getContent().get(0).getPartNumber());
    }

    @Test
    void getPartById_WhenMissing_Should404() {
        when(partRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> partService.getPartById(99L));
    }

    @Test
    void updatePart_WhenDuplicateNumber_ShouldConflict() {
        UpdatePartRequest request = validUpdateRequest();
        when(workOrderAccessGuard.requireCurrentUser(ADMIN_EMAIL)).thenReturn(admin);
        when(partRepository.findById(10L)).thenReturn(Optional.of(filter));
        when(partRepository.existsByPartNumberAndIdNot("FLT-100", 10L)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> partService.updatePart(10L, request, ADMIN_EMAIL));
    }

    @Test
    void deletePart_WhenUsedOnWorkOrder_ShouldConflict() {
        when(workOrderAccessGuard.requireCurrentUser(ADMIN_EMAIL)).thenReturn(admin);
        when(partRepository.findById(10L)).thenReturn(Optional.of(filter));
        when(workOrderPartRepository.existsByPartId(10L)).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class, () -> partService.deletePart(10L, ADMIN_EMAIL));
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(partRepository, never()).delete(any());
    }

    @Test
    void deletePart_WhenUnused_ShouldDelete() {
        when(workOrderAccessGuard.requireCurrentUser(ADMIN_EMAIL)).thenReturn(admin);
        when(partRepository.findById(10L)).thenReturn(Optional.of(filter));
        when(workOrderPartRepository.existsByPartId(10L)).thenReturn(false);

        partService.deletePart(10L, ADMIN_EMAIL);

        verify(partRepository).delete(filter);
    }

    private CreatePartRequest validCreateRequest() {
        return CreatePartRequest.builder()
                .partNumber(" flt-100 ")
                .name("HVAC Filter")
                .description("Standard filter")
                .category("Filters")
                .unitOfMeasure("EA")
                .unitCost(new BigDecimal("12.50"))
                .quantityInStock(20)
                .reorderLevel(5)
                .status(PartStatus.ACTIVE)
                .build();
    }

    private UpdatePartRequest validUpdateRequest() {
        return UpdatePartRequest.builder()
                .partNumber("FLT-100")
                .name("HVAC Filter")
                .description("Standard filter")
                .category("Filters")
                .unitOfMeasure("EA")
                .unitCost(new BigDecimal("12.50"))
                .quantityInStock(20)
                .reorderLevel(5)
                .status(PartStatus.ACTIVE)
                .build();
    }
}
