package com.keystone.service;

import com.keystone.dto.CreateSiteRequest;
import com.keystone.dto.SitePageResponse;
import com.keystone.dto.SiteResponse;
import com.keystone.dto.UpdateSiteRequest;
import com.keystone.entity.Customer;
import com.keystone.entity.Site;
import com.keystone.enums.SiteStatus;
import com.keystone.exception.ApiException;
import com.keystone.exception.DuplicateResourceException;
import com.keystone.exception.ResourceNotFoundException;
import com.keystone.repository.CustomerRepository;
import com.keystone.repository.SiteRepository;
import com.keystone.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SiteServiceImpl implements SiteService {

    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "createdAt", "updatedAt", "siteCode", "siteName", "city", "status"
    );

    private final SiteRepository siteRepository;
    private final CustomerRepository customerRepository;
    private final WorkOrderRepository workOrderRepository;

    @Override
    @Transactional(readOnly = true)
    public SitePageResponse getSites(int page, int size, String sort, String search, SiteStatus status, Long customerId) {
        Pageable pageable = buildPageable(page, size, sort);
        String searchTerm = normalizeSearch(search);

        Page<Site> sitePage = siteRepository.searchSites(searchTerm, status, customerId, pageable);

        List<SiteResponse> content = sitePage.getContent()
                .stream()
                .map(SiteResponse::fromEntity)
                .toList();

        return SitePageResponse.builder()
                .content(content)
                .page(sitePage.getNumber())
                .size(sitePage.getSize())
                .totalElements(sitePage.getTotalElements())
                .totalPages(sitePage.getTotalPages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SiteResponse getSiteById(Long id) {
        return SiteResponse.fromEntity(findSiteWithCustomer(id));
    }

    @Override
    @Transactional
    public SiteResponse createSite(CreateSiteRequest request) {
        String siteCode = normalizeCode(request.getSiteCode());
        assertSiteCodeAvailable(siteCode, null);

        Customer customer = findCustomer(request.getCustomerId());

        Site site = Site.builder()
                .siteCode(siteCode)
                .siteName(trimToNull(request.getSiteName()))
                .customer(customer)
                .addressLine1(trimToNull(request.getAddressLine1()))
                .addressLine2(trimToNull(request.getAddressLine2()))
                .city(trimToNull(request.getCity()))
                .state(trimToNull(request.getState()))
                .postalCode(trimToNull(request.getPostalCode()))
                .country(trimToNull(request.getCountry()))
                .contactName(trimToNull(request.getContactName()))
                .contactPhone(trimToNull(request.getContactPhone()))
                .contactEmail(normalizeEmail(request.getContactEmail()))
                .description(trimToNull(request.getDescription()))
                .status(SiteStatus.ACTIVE)
                .build();

        Site saved = siteRepository.save(site);
        return SiteResponse.fromEntity(findSiteWithCustomer(saved.getId()));
    }

    @Override
    @Transactional
    public SiteResponse updateSite(Long id, UpdateSiteRequest request) {
        Site site = findSiteWithCustomer(id);

        String siteCode = normalizeCode(request.getSiteCode());
        assertSiteCodeAvailable(siteCode, id);

        Customer customer = findCustomer(request.getCustomerId());

        site.setSiteCode(siteCode);
        site.setSiteName(trimToNull(request.getSiteName()));
        site.setCustomer(customer);
        site.setAddressLine1(trimToNull(request.getAddressLine1()));
        site.setAddressLine2(trimToNull(request.getAddressLine2()));
        site.setCity(trimToNull(request.getCity()));
        site.setState(trimToNull(request.getState()));
        site.setPostalCode(trimToNull(request.getPostalCode()));
        site.setCountry(trimToNull(request.getCountry()));
        site.setContactName(trimToNull(request.getContactName()));
        site.setContactPhone(trimToNull(request.getContactPhone()));
        site.setContactEmail(normalizeEmail(request.getContactEmail()));
        site.setDescription(trimToNull(request.getDescription()));

        siteRepository.save(site);
        return SiteResponse.fromEntity(findSiteWithCustomer(id));
    }

    @Override
    @Transactional
    public SiteResponse updateSiteStatus(Long id, SiteStatus status) {
        if (status == null) {
            throw new ApiException("Status is required", HttpStatus.BAD_REQUEST);
        }

        Site site = findSiteWithCustomer(id);
        site.setStatus(status);
        siteRepository.save(site);
        return SiteResponse.fromEntity(findSiteWithCustomer(id));
    }

    @Override
    @Transactional
    public void deleteSite(Long id) {
        Site site = findSiteWithCustomer(id);
        assertSiteHasNoDependencies(id);
        siteRepository.delete(site);
    }

    private void assertSiteHasNoDependencies(Long siteId) {
        if (workOrderRepository.existsBySiteId(siteId)) {
            throw new ApiException(
                    "Cannot delete site because one or more work orders are associated with it.",
                    HttpStatus.CONFLICT
            );
        }
    }

    private Site findSiteWithCustomer(Long id) {
        return siteRepository.findByIdWithCustomer(id)
                .orElseThrow(() -> new ResourceNotFoundException("Site not found with id: " + id));
    }

    private Customer findCustomer(Long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));
    }

    private void assertSiteCodeAvailable(String siteCode, Long currentId) {
        boolean exists = currentId == null
                ? siteRepository.existsBySiteCode(siteCode)
                : siteRepository.existsBySiteCodeAndIdNot(siteCode, currentId);

        if (exists) {
            throw new DuplicateResourceException("A site with code " + siteCode + " already exists.");
        }
    }

    private Pageable buildPageable(int page, int size, String sort) {
        int pageNumber = Math.max(page, 0);
        int pageSize = Math.min(Math.max(size, 1), 100);
        String sortProperty = (sort != null && SORTABLE_FIELDS.contains(sort.trim())) ? sort.trim() : "createdAt";
        return PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, sortProperty));
    }

    private String normalizeSearch(String search) {
        return (search != null && !search.isBlank()) ? search.trim() : null;
    }

    private String normalizeCode(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
