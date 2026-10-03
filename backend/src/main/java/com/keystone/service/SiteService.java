package com.keystone.service;

import com.keystone.dto.CreateSiteRequest;
import com.keystone.dto.SitePageResponse;
import com.keystone.dto.SiteResponse;
import com.keystone.dto.UpdateSiteRequest;
import com.keystone.enums.SiteStatus;

public interface SiteService {

    SitePageResponse getSites(int page, int size, String sort, String search, SiteStatus status, Long customerId);

    SiteResponse getSiteById(Long id);

    SiteResponse createSite(CreateSiteRequest request);

    SiteResponse updateSite(Long id, UpdateSiteRequest request);

    SiteResponse updateSiteStatus(Long id, SiteStatus status);

    void deleteSite(Long id);
}
