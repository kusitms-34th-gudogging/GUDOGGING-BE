package com.subscribers.gudogging.domain.catalog.infrastructure;

import com.subscribers.gudogging.domain.catalog.domain.bundle.Bundle;
import com.subscribers.gudogging.domain.catalog.domain.bundle.BundleDiscount;
import com.subscribers.gudogging.domain.catalog.domain.bundle.BundleDiscountTarget;
import com.subscribers.gudogging.domain.catalog.domain.bundle.BundleProviderType;
import com.subscribers.gudogging.domain.catalog.domain.bundle.BundleRecommendType;
import com.subscribers.gudogging.domain.catalog.domain.entity.Category;
import com.subscribers.gudogging.domain.catalog.domain.repository.BundleRepository;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;

/**
 * seed/bundles_seed.tsv(구글 시트 '번들 상품 목록' 변환)를 앱 시작 시 읽어 메모리에 보관한다. 형식이 틀리면 앱 시작을 실패시킨다.
 */
@Slf4j
@Repository
public class TsvBundleRepository implements BundleRepository {

    private static final String SEED_FILE = "seed/bundles_seed.tsv";
    private static final String HEADER = String.join("\t", "name", "recommend_type", "provider_type",
        "monthly_price", "condition", "tag1", "tag2", "fixed_services", "choice_a_services",
        "choice_b_services", "applied_prices", "discounts");
    private static final int COLUMN_COUNT = 12;
    private static final String LIST_SEPARATOR = "\\|";
    private static final String KEY_VALUE_SEPARATOR = "=";
    private static final String PERCENT = "%";

    private final List<Bundle> bundles;

    public TsvBundleRepository() {
        this.bundles = readBundles();
        log.info("번들 상품 로드: {}건", bundles.size());
    }

    @Override
    public List<Bundle> findAll() {
        return bundles;
    }

    private List<Bundle> readBundles() {
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(new ClassPathResource(SEED_FILE).getInputStream(), StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (!HEADER.equals(header)) {
                throw new IllegalStateException("번들 시드 TSV 헤더가 올바르지 않습니다: " + header);
            }
            List<Bundle> rows = new ArrayList<>();
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (!line.isBlank()) {
                    rows.add(parseRow(line, lineNumber));
                }
            }
            return List.copyOf(rows);
        } catch (IOException e) {
            throw new UncheckedIOException("번들 시드 TSV를 읽을 수 없습니다", e);
        }
    }

    private Bundle parseRow(String line, int lineNumber) {
        String[] columns = line.split("\t", -1);
        if (columns.length != COLUMN_COUNT) {
            throw new IllegalStateException("번들 시드 TSV " + lineNumber + "행의 칸 수가 올바르지 않습니다");
        }
        try {
            return new Bundle(
                columns[0],
                BundleRecommendType.valueOf(columns[1]),
                BundleProviderType.valueOf(columns[2]),
                columns[3].isEmpty() ? null : Integer.valueOf(columns[3]),
                emptyToNull(columns[4]),
                emptyToNull(columns[5]),
                emptyToNull(columns[6]),
                toList(columns[7]),
                toList(columns[8]),
                toList(columns[9]),
                toAppliedPrices(columns[10]),
                toDiscounts(columns[11])
            );
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("번들 시드 TSV " + lineNumber + "행을 해석할 수 없습니다", e);
        }
    }

    private List<String> toList(String value) {
        if (value.isEmpty()) {
            return List.of();
        }
        return Arrays.stream(value.split(LIST_SEPARATOR)).map(String::trim).toList();
    }

    private Map<String, Integer> toAppliedPrices(String value) {
        Map<String, Integer> prices = new LinkedHashMap<>();
        for (String entry : toList(value)) {
            String[] pair = splitPair(entry);
            prices.put(pair[0], Integer.valueOf(pair[1]));
        }
        return prices;
    }

    /**
     * 'ALL=30%' / 'A=50%|B=3000' / 'FIXED=10%|A=50%' / 'PRODUCTIVITY_AI=10%|SHOPPING_DELIVERY=50%' — 숫자 뒤 %면 할인율, 아니면 할인액(원).
     */
    private List<BundleDiscount> toDiscounts(String value) {
        List<BundleDiscount> discounts = new ArrayList<>();
        for (String entry : toList(value)) {
            String[] pair = splitPair(entry);
            BundleDiscountTarget target;
            Category category = null;
            try {
                target = BundleDiscountTarget.valueOf(pair[0]);
            } catch (IllegalArgumentException e) {
                target = BundleDiscountTarget.CATEGORY;
                category = Category.valueOf(pair[0]);
            }
            if (target == BundleDiscountTarget.CATEGORY && category == null) {
                throw new IllegalArgumentException("카테고리 할인 대상이 없습니다: " + entry);
            }
            boolean isPercent = pair[1].endsWith(PERCENT);
            int number = Integer.parseInt(isPercent ? pair[1].substring(0, pair[1].length() - 1) : pair[1]);
            discounts.add(new BundleDiscount(target, category, isPercent ? number : null, isPercent ? null : number));
        }
        return discounts;
    }

    private String[] splitPair(String entry) {
        int index = entry.lastIndexOf(KEY_VALUE_SEPARATOR);
        if (index <= 0) {
            throw new IllegalArgumentException("'이름=값' 형식이 아닙니다: " + entry);
        }
        return new String[]{entry.substring(0, index).trim(), entry.substring(index + 1).trim()};
    }

    private String emptyToNull(String value) {
        return value.isEmpty() ? null : value;
    }
}
