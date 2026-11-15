package com.subscribers.gudogging.domain.user.domain.service;

import com.subscribers.gudogging.domain.user.domain.entity.SocialProvider;
import com.subscribers.gudogging.domain.user.domain.entity.User;
import com.subscribers.gudogging.domain.user.domain.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserGetService {

    private final UserRepository userRepository;

    public Optional<User> findUserBySocialAccount(SocialProvider socialProvider, String socialId) {
        return userRepository.findBySocialProviderAndSocialIdAndDeletedFalse(socialProvider, socialId);
    }

    public Optional<User> findUserById(UUID userId) {
        return userRepository.findByIdAndDeletedFalse(userId);
    }
}
