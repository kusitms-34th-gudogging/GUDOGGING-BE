package com.subscribers.gudogging.domain.catalog.domain.service;

import com.subscribers.gudogging.domain.catalog.domain.entity.ServiceCatalog;
import com.subscribers.gudogging.domain.catalog.domain.repository.ServiceCatalogRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ServiceCatalogGetService {

    private final ServiceCatalogRepository serviceCatalogRepository;

    public List<ServiceCatalog> findActiveServices() {
        return serviceCatalogRepository.findAllByIsActiveTrueAndDeletedFalseOrderBySortOrderAscNameAsc();
    }
}
