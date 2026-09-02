package com.tinsae.socialmediaplatform.report.repository;

import com.tinsae.socialmediaplatform.common.enums.ReportStatus;
import com.tinsae.socialmediaplatform.common.enums.ReportTargetType;
import com.tinsae.socialmediaplatform.report.entity.Report;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, UUID> {

    List<Report> findByReporterIdOrderByCreatedAtDesc(UUID reporterId, Pageable pageable);

    boolean existsByReporterIdAndTargetTypeAndTargetIdAndStatus(
            UUID reporterId,
            ReportTargetType targetType,
            UUID targetId,
            ReportStatus status
    );

    long countByStatus(ReportStatus status);

    long countByTargetType(ReportTargetType targetType);

    @Query("""
            select r from Report r
            where (:status is null or r.status = :status)
              and (:cursor is null or r.createdAt > :cursor)
            order by r.createdAt asc
            """)
    List<Report> findAdminReports(
            @Param("status") ReportStatus status,
            @Param("cursor") Instant cursor,
            Pageable pageable
    );
}
