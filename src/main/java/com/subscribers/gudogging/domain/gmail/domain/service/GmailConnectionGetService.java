package com.subscribers.gudogging.domain.gmail.domain.service;

import com.subscribers.gudogging.domain.gmail.domain.entity.GmailConnection;
import com.subscribers.gudogging.domain.gmail.domain.repository.GmailConnectionRepository;
import com.subscribers.gudogging.domain.user.domain.entity.User;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GmailConnectionGetService {

    private final GmailConnectionRepository gmailConnectionRepository;

    /**
     * 회원의 활성(연결 해제되지 않은) Gmail 연결을 조회한다. 회원당 활성 연결은 최대 1개다.
     */
    public Optional<GmailConnection> findGmailConnection(User user) {
        return gmailConnectionRepository.findByUserAndDeletedFalse(user);
    }
}
