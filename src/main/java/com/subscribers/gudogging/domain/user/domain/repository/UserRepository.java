package com.subscribers.gudogging.domain.user.domain.repository;

import com.subscribers.gudogging.domain.user.domain.entity.SocialProvider;
import com.subscribers.gudogging.domain.user.domain.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findBySocialProviderAndSocialIdAndDeletedFalse(SocialProvider socialProvider, String socialId);
}
