package com.subscribers.gudogging.domain.catalog.presentation;

import com.subscribers.gudogging.domain.catalog.application.dto.request.ServiceSeedRow;
import com.subscribers.gudogging.domain.catalog.application.usecase.CatalogUseCase;
import com.subscribers.gudogging.domain.catalog.domain.entity.Category;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * 앱 시작 시 services_seed.csv를 읽어 services 테이블과 동기화한다(새 서비스 추가 · 기존 서비스 갱신 · 빠진 서비스 비활성화는 CatalogUseCase).
 * logo_url이 '/'로 시작하면 우리 서버가 제공하는 정적 파일(static/logos)이므로 server-uri를 앞에 붙여 전체 URL로 저장한다.
 */
@Component
public class ServiceCatalogSeedRunner implements ApplicationRunner {

    private static final String SEED_FILE = "seed/services_seed.csv";
    private static final String HEADER = "name,category,logo_url,cancel_url,default_amount,sort_order";
    private static final String BOM = "﻿";
    private static final int COLUMN_COUNT = 6;

    private final CatalogUseCase catalogUseCase;
    private final String serverUri;

    public ServiceCatalogSeedRunner(CatalogUseCase catalogUseCase, @Value("${server-uri}") String serverUri) {
        this.catalogUseCase = catalogUseCase;
        this.serverUri = serverUri.endsWith("/") ? serverUri.substring(0, serverUri.length() - 1) : serverUri;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        catalogUseCase.seedServices(readRows());
    }

    private List<ServiceSeedRow> readRows() throws IOException {
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(new ClassPathResource(SEED_FILE).getInputStream(), StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header == null || !HEADER.equals(stripBom(header))) {
                throw new IllegalStateException("서비스 시드 CSV 헤더가 올바르지 않습니다: " + header);
            }

            List<ServiceSeedRow> rows = new ArrayList<>();
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (!line.isBlank()) {
                    rows.add(parseRow(line, lineNumber));
                }
            }
            return rows;
        }
    }

    private ServiceSeedRow parseRow(String line, int lineNumber) {
        String[] columns = line.split(",", -1);
        if (columns.length != COLUMN_COUNT) {
            throw new IllegalStateException("서비스 시드 CSV " + lineNumber + "행의 컬럼 수가 올바르지 않습니다: " + line);
        }
        try {
            return new ServiceSeedRow(
                columns[0],
                Category.valueOf(columns[1]),
                toLogoUrl(columns[2]),
                emptyToNull(columns[3]),
                columns[4].isEmpty() ? null : Integer.valueOf(columns[4]),
                Integer.parseInt(columns[5])
            );
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("서비스 시드 CSV " + lineNumber + "행을 해석할 수 없습니다: " + line, e);
        }
    }

    private String stripBom(String text) {
        return text.startsWith(BOM) ? text.substring(BOM.length()) : text;
    }

    private String toLogoUrl(String value) {
        return value.startsWith("/") ? serverUri + value : emptyToNull(value);
    }

    private String emptyToNull(String value) {
        return value.isEmpty() ? null : value;
    }
}
