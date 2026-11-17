package com.subscribers.gudogging.domain.catalog.domain.service;

import com.subscribers.gudogging.domain.catalog.domain.entity.ServiceCatalog;
import com.subscribers.gudogging.domain.catalog.domain.repository.ServiceCatalogRepository;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ServiceCatalogGetService {

    private final ServiceCatalogRepository serviceCatalogRepository;

    public List<ServiceCatalog> findActiveServices() {
        return serviceCatalogRepository.findAllByIsActiveTrueAndDeletedFalseOrderBySortOrderAscNameAsc();
    }

    public List<ServiceCatalog> findActiveServicesByIds(Collection<UUID> serviceIds) {
        return serviceCatalogRepository.findAllByIdInAndIsActiveTrueAndDeletedFalse(serviceIds);
    }
}
