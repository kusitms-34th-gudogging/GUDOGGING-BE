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

    /**
     * 삭제(soft delete)·비활성 서비스를 포함한 전체 서비스를 조회한다 (시드 동기화용).
     */
    public List<ServiceCatalog> findAllServices() {
        return serviceCatalogRepository.findAll();
    }

    public List<ServiceCatalog> findActiveServicesByIds(Collection<UUID> serviceIds) {
        return serviceCatalogRepository.findAllByIdInAndIsActiveTrueAndDeletedFalse(serviceIds);
    }
}
