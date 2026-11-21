package com.subscribers.gudogging.domain.catalog.domain.repository;

import com.subscribers.gudogging.domain.catalog.domain.bundle.Bundle;
import java.util.List;

/**
 * 번들 상품 조회 — DB 테이블 없이 시드 파일에서 읽어 메모리에 보관한다 (CATALOG-003).
 */
public interface BundleRepository {

    List<Bundle> findAll();
}
