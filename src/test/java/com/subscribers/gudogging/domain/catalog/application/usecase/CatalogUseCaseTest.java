package com.subscribers.gudogging.domain.catalog.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import com.subscribers.gudogging.domain.catalog.application.dto.response.ServiceResponse;
import com.subscribers.gudogging.domain.catalog.domain.entity.Category;
import com.subscribers.gudogging.domain.catalog.domain.entity.ServiceCatalog;
import com.subscribers.gudogging.domain.catalog.domain.service.ServiceCatalogGetService;
import com.subscribers.gudogging.domain.subscription.domain.service.SubscriptionGetService;
import com.subscribers.gudogging.domain.user.application.exception.UserErrorCode;
import com.subscribers.gudogging.domain.user.application.exception.UserNotFoundException;
import com.subscribers.gudogging.domain.user.domain.entity.SocialProvider;
import com.subscribers.gudogging.domain.user.domain.entity.User;
import com.subscribers.gudogging.domain.user.domain.service.UserGetService;
import com.subscribers.gudogging.global.common.exception.ApplicationException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("CatalogUseCase")
class CatalogUseCaseTest {

    @Mock
    private UserGetService userGetService;

    @Mock
    private SubscriptionGetService subscriptionGetService;

    @Mock
    private ServiceCatalogGetService serviceCatalogGetService;

    @InjectMocks
    private CatalogUseCase catalogUseCase;

    private static ServiceCatalog service(String name, String nameEn, String searchKeywords, Category category,
                                          int sortOrder) {
        ServiceCatalog service = ServiceCatalog.create(name, nameEn, searchKeywords, category,
            "https://logo.example.com/" + sortOrder + ".png", sortOrder);
        // id는 JPA 저장 시 생성되므로, 구독 중 서비스 제외(ID 비교) 검증을 위해 테스트에서만 지정한다.
        ReflectionTestUtils.setField(service, "id", UUID.randomUUID());
        return service;
    }

    private static List<String> namesOf(List<ServiceResponse> responses) {
        return responses.stream().map(ServiceResponse::name).toList();
    }

    @Nested
    @DisplayName("getServices")
    class GetServices {

        private final UUID userId = UUID.randomUUID();
        private final User user = User.create(SocialProvider.GOOGLE, "social-sub-123", "user@example.com", "홍길동");

        private final ServiceCatalog netflix = service("넷플릭스", "Netflix", "넷플,netflix", Category.VIDEO_OTT, 1);
        private final ServiceCatalog youtube = service("유튜브 프리미엄", "YouTube Premium", "유튭,youtube", Category.VIDEO_OTT,
            2);
        private final ServiceCatalog melon = service("멜론", "Melon", null, Category.MUSIC, 3);
        private final ServiceCatalog spotify = service("스포티파이", "Spotify", null, Category.MUSIC, 4);

        private void givenServices(Set<UUID> registeredServiceIds, ServiceCatalog... services) {
            given(userGetService.findUserById(userId)).willReturn(Optional.of(user));
            given(subscriptionGetService.findActiveServiceIds(user)).willReturn(registeredServiceIds);
            given(serviceCatalogGetService.findActiveServices()).willReturn(List.of(services));
        }

        @Test
        @DisplayName("keyword가 null이면 노출 서비스 전체를 조회 순서 그대로 반환한다")
        void returnsAllServicesWhenKeywordIsNull() {
            // given
            givenServices(Set.of(), netflix, youtube, melon, spotify);

            // when
            List<ServiceResponse> responses = catalogUseCase.getServices(userId, null);

            // then
            assertThat(namesOf(responses)).containsExactly("넷플릭스", "유튜브 프리미엄", "멜론", "스포티파이");
            ServiceResponse first = responses.getFirst();
            assertThat(first.serviceId()).isEqualTo(netflix.getId());
            assertThat(first.category()).isEqualTo(Category.VIDEO_OTT);
            assertThat(first.logoUrl()).isEqualTo(netflix.getLogoUrl());
        }

        @Test
        @DisplayName("keyword가 공백만 있으면 전체 목록을 반환한다")
        void returnsAllServicesWhenKeywordIsBlank() {
            // given
            givenServices(Set.of(), netflix, youtube, melon);

            // when
            List<ServiceResponse> responses = catalogUseCase.getServices(userId, "   ");

            // then
            assertThat(namesOf(responses)).containsExactly("넷플릭스", "유튜브 프리미엄", "멜론");
        }

        @Test
        @DisplayName("회원이 ACTIVE로 구독 중인 서비스는 전체 목록에서 제외한다")
        void excludesRegisteredServicesFromAll() {
            // given
            givenServices(Set.of(netflix.getId(), melon.getId()), netflix, youtube, melon, spotify);

            // when
            List<ServiceResponse> responses = catalogUseCase.getServices(userId, null);

            // then
            assertThat(namesOf(responses)).containsExactly("유튜브 프리미엄", "스포티파이");
        }

        @Test
        @DisplayName("회원이 ACTIVE로 구독 중인 서비스는 검색어와 일치해도 검색 결과에서 제외한다")
        void excludesRegisteredServicesFromSearch() {
            // given
            givenServices(Set.of(netflix.getId()), netflix, youtube, melon);

            // when
            List<ServiceResponse> responses = catalogUseCase.getServices(userId, "넷플");

            // then
            assertThat(responses).isEmpty();
        }

        @Test
        @DisplayName("'넷플'로 검색하면 서비스명 접두 일치로 넷플릭스를 반환한다")
        void searchesByNamePrefix() {
            // given
            givenServices(Set.of(), netflix, youtube, melon);

            // when
            List<ServiceResponse> responses = catalogUseCase.getServices(userId, "넷플");

            // then
            assertThat(namesOf(responses)).containsExactly("넷플릭스");
        }

        @Test
        @DisplayName("'ㄴㅍ'로 검색하면 초성 일치로 넷플릭스를 반환한다")
        void searchesByChosung() {
            // given
            givenServices(Set.of(), netflix, youtube, melon);

            // when
            List<ServiceResponse> responses = catalogUseCase.getServices(userId, "ㄴㅍ");

            // then
            assertThat(namesOf(responses)).containsExactly("넷플릭스");
        }

        @Test
        @DisplayName("'NETFLIX'로 검색하면 대소문자를 무시하고 name_en 일치로 넷플릭스를 반환한다")
        void searchesByEnglishNameIgnoringCase() {
            // given
            givenServices(Set.of(), netflix, youtube, melon);

            // when
            List<ServiceResponse> responses = catalogUseCase.getServices(userId, "NETFLIX");

            // then
            assertThat(namesOf(responses)).containsExactly("넷플릭스");
        }

        @Test
        @DisplayName("search_keywords 별칭으로 검색하면 해당 서비스를 반환한다")
        void searchesBySearchKeyword() {
            // given
            givenServices(Set.of(), netflix, youtube, melon);

            // when
            List<ServiceResponse> responses = catalogUseCase.getServices(userId, "유튭");

            // then
            assertThat(namesOf(responses)).containsExactly("유튜브 프리미엄");
        }

        @Test
        @DisplayName("'음악'으로 검색하면 MUSIC 카테고리 서비스를 반환한다")
        void searchesByCategoryName() {
            // given
            givenServices(Set.of(), netflix, youtube, melon, spotify);

            // when
            List<ServiceResponse> responses = catalogUseCase.getServices(userId, "음악");

            // then
            assertThat(namesOf(responses)).containsExactly("멜론", "스포티파이");
        }

        @Test
        @DisplayName("정확 일치 > 접두 일치 > 부분 일치 순으로 정렬한다")
        void ranksExactThenPrefixThenContains() {
            // given
            ServiceCatalog contains = service("에이티비", null, null, Category.ETC, 1);
            ServiceCatalog prefix = service("티비플러스", null, null, Category.ETC, 2);
            ServiceCatalog exact = service("티비", null, null, Category.ETC, 3);
            givenServices(Set.of(), contains, prefix, exact);

            // when
            List<ServiceResponse> responses = catalogUseCase.getServices(userId, "티비");

            // then
            assertThat(namesOf(responses)).containsExactly("티비", "티비플러스", "에이티비");
        }

        @Test
        @DisplayName("점수가 같으면 sort_order 순으로 정렬한다")
        void ranksBySortOrderWhenScoreIsEqual() {
            // given
            ServiceCatalog laterOrder = service("티비플러스", null, null, Category.ETC, 2);
            ServiceCatalog earlierOrder = service("티비나라", null, null, Category.ETC, 0);
            givenServices(Set.of(), laterOrder, earlierOrder);

            // when
            List<ServiceResponse> responses = catalogUseCase.getServices(userId, "티비");

            // then
            assertThat(namesOf(responses)).containsExactly("티비나라", "티비플러스");
        }

        @Test
        @DisplayName("초성 검색은 초성 접두 일치를 초성 부분 일치보다 먼저 반환한다")
        void ranksChosungPrefixBeforeChosungContains() {
            // given
            ServiceCatalog chosungContains = service("에이티비", null, null, Category.ETC, 1);
            ServiceCatalog chosungPrefix = service("타블로", null, null, Category.ETC, 2);
            givenServices(Set.of(), chosungContains, chosungPrefix);

            // when
            List<ServiceResponse> responses = catalogUseCase.getServices(userId, "ㅌㅂ");

            // then
            assertThat(namesOf(responses)).containsExactly("타블로", "에이티비");
        }

        @Test
        @DisplayName("서비스명 일치를 카테고리명 일치보다 먼저 반환한다")
        void ranksNameMatchBeforeCategoryMatch() {
            // given
            ServiceCatalog categoryOnly = service("티비", null, null, Category.ETC, 1);
            ServiceCatalog nameMatch = service("기타서비스", null, null, Category.ETC, 2);
            givenServices(Set.of(), categoryOnly, nameMatch);

            // when
            List<ServiceResponse> responses = catalogUseCase.getServices(userId, "기타");

            // then
            assertThat(namesOf(responses)).containsExactly("기타서비스", "티비");
        }

        @Test
        @DisplayName("매칭되는 서비스가 없으면 빈 목록을 반환한다")
        void returnsEmptyWhenNoMatch() {
            // given
            givenServices(Set.of(), netflix, youtube, melon);

            // when
            List<ServiceResponse> responses = catalogUseCase.getServices(userId, "존재하지않는서비스");

            // then
            assertThat(responses).isEmpty();
        }

        @Test
        @DisplayName("회원이 없거나 탈퇴했으면 USER_NOT_FOUND 예외를 던진다")
        void throwsWhenUserNotFound() {
            // given
            given(userGetService.findUserById(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> catalogUseCase.getServices(userId, null))
                .isInstanceOf(UserNotFoundException.class)
                .extracting(e -> ((ApplicationException) e).getErrorCode())
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
            verifyNoInteractions(subscriptionGetService, serviceCatalogGetService);
        }
    }
}
