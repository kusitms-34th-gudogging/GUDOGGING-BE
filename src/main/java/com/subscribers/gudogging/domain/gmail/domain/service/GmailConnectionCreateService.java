package com.subscribers.gudogging.domain.gmail.domain.service;

import com.subscribers.gudogging.domain.gmail.domain.entity.GmailConnection;
import com.subscribers.gudogging.domain.gmail.domain.repository.GmailConnectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GmailConnectionCreateService {

    private final GmailConnectionRepository gmailConnectionRepository;

    public GmailConnection saveGmailConnection(GmailConnection gmailConnection) {
        return gmailConnectionRepository.save(gmailConnection);
    }
}
