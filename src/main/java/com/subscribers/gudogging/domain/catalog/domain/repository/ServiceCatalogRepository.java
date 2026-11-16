package com.subscribers.gudogging.domain.catalog.domain.repository;

import com.subscribers.gudogging.domain.catalog.domain.entity.ServiceCatalog;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceCatalogRepository extends JpaRepository<ServiceCatalog, UUID> {
}
