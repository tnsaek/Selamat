package com.tinsae.socialmediaplatform.admin.repository;

import com.tinsae.socialmediaplatform.admin.entity.ModerationAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ModerationAuditLogRepository extends JpaRepository<ModerationAuditLog, UUID> {
}
