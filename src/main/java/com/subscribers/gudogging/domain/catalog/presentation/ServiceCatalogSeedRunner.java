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
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * 앱 시작 시 services_seed.csv를 읽어 서비스 초기 데이터를 적재한다. 적재 여부 판단(테이블이 비어 있을 때만)은 CatalogUseCase가 한다.
 */
@Component
@RequiredArgsConstructor
public class ServiceCatalogSeedRunner implements ApplicationRunner {

    private static final String SEED_FILE = "seed/services_seed.csv";
    private static final String HEADER = "name,category,logo_url,cancel_url,sort_order";
    private static final String BOM = "﻿";
    private static final int COLUMN_COUNT = 5;

    private final CatalogUseCase catalogUseCase;

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
                emptyToNull(columns[2]),
                emptyToNull(columns[3]),
                Integer.parseInt(columns[4])
            );
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("서비스 시드 CSV " + lineNumber + "행을 해석할 수 없습니다: " + line, e);
        }
    }

    private String stripBom(String text) {
        return text.startsWith(BOM) ? text.substring(BOM.length()) : text;
    }

    private String emptyToNull(String value) {
        return value.isEmpty() ? null : value;
    }
}
