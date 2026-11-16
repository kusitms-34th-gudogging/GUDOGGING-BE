package com.subscribers.gudogging.domain.gmail.domain.repository;

import com.subscribers.gudogging.domain.gmail.domain.entity.GmailConnection;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GmailConnectionRepository extends JpaRepository<GmailConnection, UUID> {
}
