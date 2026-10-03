package com.keystone.service;

import com.keystone.dto.CreatePartRequest;
import com.keystone.dto.PartPageResponse;
import com.keystone.dto.PartResponse;
import com.keystone.dto.UpdatePartRequest;
import com.keystone.enums.PartStatus;

public interface PartService {

    PartPageResponse getParts(int page, int size, String sort, String search, String category, PartStatus status);

    PartResponse getPartById(Long id);

    PartResponse createPart(CreatePartRequest request, String currentUsername);

    PartResponse updatePart(Long id, UpdatePartRequest request, String currentUsername);

    void deletePart(Long id, String currentUsername);
}
