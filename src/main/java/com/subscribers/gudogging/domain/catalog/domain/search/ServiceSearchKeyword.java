package com.subscribers.gudogging.domain.catalog.domain.search;

import com.subscribers.gudogging.domain.catalog.domain.entity.ServiceCatalog;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 직접 등록 서비스 검색어 (A-02s). 서비스명·영문명·별칭·초성·카테고리명으로 매칭하고 일치율이 높은 순으로 정렬한다.
 */
public final class ServiceSearchKeyword {

    private static final int EXACT_SCORE = 100;
    private static final int PREFIX_SCORE = 80;
    private static final int CONTAINS_SCORE = 60;
    private static final int CHOSUNG_PREFIX_SCORE = 50;
    private static final int CHOSUNG_CONTAINS_SCORE = 40;
    private static final int CATEGORY_SCORE = 30;
    private static final int NO_MATCH = 0;

    private static final char HANGUL_SYLLABLE_START = '가';
    private static final char HANGUL_SYLLABLE_END = '힣';
    private static final int SYLLABLES_PER_CHOSUNG = 588;
    private static final String CHOSUNGS = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ";

    private static final String KEYWORD_DELIMITER = ",";
    private static final String CATEGORY_SEPARATOR = "·";

    private static final Comparator<ScoredService> RANKING = Comparator
        .comparingInt(ScoredService::score).reversed()
        .thenComparingInt(scored -> scored.service().getSortOrder())
        .thenComparing(scored -> scored.service().getName());

    private final String value;
    private final boolean chosungOnly;

    private ServiceSearchKeyword(String value) {
        this.value = value;
        this.chosungOnly = !value.isEmpty() && value.chars().allMatch(c -> CHOSUNGS.indexOf(c) >= 0);
    }

    public static ServiceSearchKeyword from(String keyword) {
        return new ServiceSearchKeyword(normalize(keyword));
    }

    /**
     * 검색어가 비어 있으면 후보를 조회 순서 그대로 반환하고, 아니면 매칭된 서비스만 점수 DESC → sort_order ASC → name ASC 순으로 반환한다.
     */
    public List<ServiceCatalog> search(List<ServiceCatalog> candidates) {
        if (value.isEmpty()) {
            return candidates;
        }
        return candidates.stream()
            .map(service -> new ScoredService(service, scoreOf(service)))
            .filter(scored -> scored.score() > NO_MATCH)
            .sorted(RANKING)
            .map(ScoredService::service)
            .toList();
    }

    private int scoreOf(ServiceCatalog service) {
        int score = NO_MATCH;
        for (String text : searchTexts(service)) {
            score = Math.max(score, textScore(text));
        }
        if (chosungOnly) {
            score = Math.max(score, chosungScore(toChosung(normalize(service.getName()))));
        }
        String categoryName = normalize(service.getCategory().getDisplayName()).replace(CATEGORY_SEPARATOR, "");
        if (categoryName.contains(value)) {
            score = Math.max(score, CATEGORY_SCORE);
        }
        return score;
    }

    private List<String> searchTexts(ServiceCatalog service) {
        List<String> texts = new ArrayList<>();
        texts.add(normalize(service.getName()));
        if (service.getNameEn() != null) {
            texts.add(normalize(service.getNameEn()));
        }
        if (service.getSearchKeywords() != null) {
            Arrays.stream(service.getSearchKeywords().split(KEYWORD_DELIMITER))
                .map(ServiceSearchKeyword::normalize)
                .filter(keyword -> !keyword.isEmpty())
                .forEach(texts::add);
        }
        return texts;
    }

    private int textScore(String text) {
        if (text.equals(value)) {
            return EXACT_SCORE;
        }
        if (text.startsWith(value)) {
            return PREFIX_SCORE;
        }
        if (text.contains(value)) {
            return CONTAINS_SCORE;
        }
        return NO_MATCH;
    }

    private int chosungScore(String chosungName) {
        if (chosungName.startsWith(value)) {
            return CHOSUNG_PREFIX_SCORE;
        }
        if (chosungName.contains(value)) {
            return CHOSUNG_CONTAINS_SCORE;
        }
        return NO_MATCH;
    }

    private static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.strip().replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    private static String toChosung(String text) {
        StringBuilder chosung = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            if (c >= HANGUL_SYLLABLE_START && c <= HANGUL_SYLLABLE_END) {
                chosung.append(CHOSUNGS.charAt((c - HANGUL_SYLLABLE_START) / SYLLABLES_PER_CHOSUNG));
            } else {
                chosung.append(c);
            }
        }
        return chosung.toString();
    }

    private record ScoredService(ServiceCatalog service, int score) {
    }
}
