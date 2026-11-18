package com.subscribers.gudogging.domain.subscription.application.dto.response;

import com.subscribers.gudogging.domain.catalog.domain.entity.ServiceCatalog;
import com.subscribers.gudogging.domain.subscription.domain.entity.CancelGuideType;
import com.subscribers.gudogging.domain.subscription.domain.entity.PaymentMethod;
import com.subscribers.gudogging.domain.subscription.domain.entity.Subscription;
import java.util.List;

/**
 * 결제 경로별 해지 안내 (X-01 / X-03 / X-04 / X-05). 버튼 문구 · 화면 제목은 클라이언트가 처리한다.
 */
public record CancelGuideResponse(
    String serviceName,
    PaymentMethod paymentMethod,
    List<String> steps,
    String cancelUrl,
    String caution
) {

    public static CancelGuideResponse from(Subscription subscription) {
        CancelGuideType guideType = subscription.cancelGuideType();
        ServiceCatalog serviceCatalog = subscription.getServiceCatalog();
        String serviceCancelUrl = serviceCatalog != null ? serviceCatalog.getCancelUrl() : null;
        return new CancelGuideResponse(
            subscription.getName(),
            subscription.getPaymentMethod(),
            guideType.steps(subscription.getName(), subscription.objectParticle()),
            guideType.cancelUrl(serviceCancelUrl),
            guideType.caution()
        );
    }
}
