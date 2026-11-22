package com.subscribers.gudogging.domain.gmail.application.dto.response;

import com.subscribers.gudogging.domain.gmail.domain.GmailCandidate;
import java.util.List;

public record GmailAnalysisResponse(
    List<GmailCandidateResponse> candidates
) {

    public static GmailAnalysisResponse from(List<GmailCandidate> candidates) {
        return new GmailAnalysisResponse(candidates.stream()
            .map(GmailCandidateResponse::from)
            .toList());
    }
}
