package com.tinsae.socialmediaplatform.report.service;

import com.tinsae.socialmediaplatform.comment.repository.CommentRepository;
import com.tinsae.socialmediaplatform.common.dto.PageResponse;
import com.tinsae.socialmediaplatform.common.enums.CommentStatus;
import com.tinsae.socialmediaplatform.common.enums.NotificationType;
import com.tinsae.socialmediaplatform.common.enums.PostStatus;
import com.tinsae.socialmediaplatform.common.enums.ReportStatus;
import com.tinsae.socialmediaplatform.common.enums.ReportTargetType;
import com.tinsae.socialmediaplatform.common.exception.BusinessRuleException;
import com.tinsae.socialmediaplatform.common.exception.ResourceNotFoundException;
import com.tinsae.socialmediaplatform.common.exception.UnauthorizedActionException;
import com.tinsae.socialmediaplatform.common.mapper.ReportMapper;
import com.tinsae.socialmediaplatform.common.mapper.UserMapper;
import com.tinsae.socialmediaplatform.common.util.CursorUtils;
import com.tinsae.socialmediaplatform.comment.entity.Comment;
import com.tinsae.socialmediaplatform.media.entity.Media;
import com.tinsae.socialmediaplatform.message.entity.Message;
import com.tinsae.socialmediaplatform.media.repository.MediaRepository;
import com.tinsae.socialmediaplatform.message.repository.MessageRepository;
import com.tinsae.socialmediaplatform.notification.service.NotificationService;
import com.tinsae.socialmediaplatform.post.entity.Post;
import com.tinsae.socialmediaplatform.post.repository.PostRepository;
import com.tinsae.socialmediaplatform.report.dto.AdminReportSummaryResponse;
import com.tinsae.socialmediaplatform.report.dto.CreateReportRequest;
import com.tinsae.socialmediaplatform.report.dto.ReportResponse;
import com.tinsae.socialmediaplatform.report.dto.ReportTargetResponse;
import com.tinsae.socialmediaplatform.report.dto.ResolveReportRequest;
import com.tinsae.socialmediaplatform.report.entity.Report;
import com.tinsae.socialmediaplatform.report.repository.ReportRepository;
import com.tinsae.socialmediaplatform.profile.entity.Profile;
import com.tinsae.socialmediaplatform.user.entity.User;
import com.tinsae.socialmediaplatform.user.repository.UserRepository;
import com.tinsae.socialmediaplatform.user.service.UserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ReportService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final CommentRepository commentRepository;
    private final MediaRepository mediaRepository;
    private final MessageRepository messageRepository;
    private final NotificationService notificationService;
    private final PostRepository postRepository;
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    public ReportService(
            CommentRepository commentRepository,
            MediaRepository mediaRepository,
            MessageRepository messageRepository,
            NotificationService notificationService,
            PostRepository postRepository,
            ReportRepository reportRepository,
            UserRepository userRepository,
            UserService userService
    ) {
        this.commentRepository = commentRepository;
        this.mediaRepository = mediaRepository;
        this.messageRepository = messageRepository;
        this.notificationService = notificationService;
        this.postRepository = postRepository;
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @Transactional
    public ReportResponse createReport(CreateReportRequest request, Jwt jwt) {
        User reporter = userService.getAuthenticatedUser(jwt);
        validateReportableTarget(request.targetType(), request.targetId(), reporter);

        if (request.targetType() == ReportTargetType.USER && reporter.getId().equals(request.targetId())) {
            throw new BusinessRuleException("You cannot report yourself.");
        }

        boolean duplicateOpenReport = reportRepository.existsByReporterIdAndTargetTypeAndTargetIdAndStatus(
                reporter.getId(),
                request.targetType(),
                request.targetId(),
                ReportStatus.OPEN
        );
        if (duplicateOpenReport) {
            throw new BusinessRuleException("You already have an open report for this target.");
        }

        Report report = new Report();
        report.setReporter(reporter);
        report.setTargetType(request.targetType());
        report.setTargetId(request.targetId());
        report.setReason(request.reason());
        report.setDetails(request.details());
        report.setStatus(ReportStatus.OPEN);

        Report savedReport = reportRepository.save(report);
        notifyModerators(savedReport, reporter);
        return ReportMapper.toResponse(savedReport);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReportResponse> listReports(ReportStatus status, String cursor, Integer limit) {
        int pageSize = normalizeLimit(limit);
        Pageable pageable = PageRequest.of(0, pageSize + 1);
        Instant cursorInstant = CursorUtils.parseCursor(cursor);

        List<Report> reports = new ArrayList<>(reportRepository.findAdminReports(status, cursorInstant, pageable));
        String nextCursor = CursorUtils.trimAndNextCursor(reports, pageSize, Report::getCreatedAt);

        List<ReportResponse> responses = reports.stream()
                .map(ReportMapper::toResponse)
                .toList();

        return new PageResponse<>(responses, nextCursor);
    }

    @Transactional(readOnly = true)
    public ReportResponse getReportById(UUID reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found."));

        return ReportMapper.toResponse(report);
    }

    @Transactional(readOnly = true)
    public AdminReportSummaryResponse getAdminSummary() {
        long postReports = reportRepository.countByTargetType(ReportTargetType.POST);
        long commentReports = reportRepository.countByTargetType(ReportTargetType.COMMENT);
        long messageReports = reportRepository.countByTargetType(ReportTargetType.MESSAGE);

        return new AdminReportSummaryResponse(
                reportRepository.countByStatus(ReportStatus.OPEN),
                reportRepository.countByStatus(ReportStatus.UNDER_REVIEW),
                reportRepository.countByStatus(ReportStatus.RESOLVED),
                reportRepository.countByStatus(ReportStatus.REJECTED),
                reportRepository.countByTargetType(ReportTargetType.USER),
                postReports + commentReports + messageReports,
                reportRepository.countByTargetType(ReportTargetType.MEDIA)
        );
    }

    @Transactional(readOnly = true)
    public ReportTargetResponse getReportTarget(UUID reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found."));

        return switch (report.getTargetType()) {
            case USER -> userTarget(report);
            case POST -> postTarget(report);
            case COMMENT -> commentTarget(report);
            case MESSAGE -> messageTarget(report);
            case MEDIA -> mediaTarget(report);
        };
    }

    @Transactional
    public ReportResponse resolveReport(UUID reportId, ResolveReportRequest request, Jwt jwt) {
        User resolver = userService.getAuthenticatedUser(jwt);
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found."));

        if (report.getStatus() == ReportStatus.RESOLVED || report.getStatus() == ReportStatus.REJECTED) {
            throw new BusinessRuleException("Report has already been resolved.");
        }

        if (request.status() == ReportStatus.OPEN || request.status() == ReportStatus.UNDER_REVIEW) {
            throw new BusinessRuleException("Report can only be resolved as RESOLVED or REJECTED.");
        }

        report.setResolver(resolver);
        report.setStatus(request.status());
        report.setResolutionNote(request.resolutionNote());
        report.setResolvedAt(Instant.now());

        Report savedReport = reportRepository.save(report);
        notificationService.createNotification(
                savedReport.getReporter(),
                resolver,
                NotificationType.REPORT_UPDATE,
                "Report updated",
                "Your report has been " + savedReport.getStatus().name().toLowerCase() + ".",
                "REPORT",
                savedReport.getId()
        );

        return ReportMapper.toResponse(savedReport);
    }

    private void notifyModerators(Report report, User reporter) {
        userRepository.findAll()
                .stream()
                .filter(user -> user.getRoles()
                        .stream()
                        .anyMatch(userRole -> {
                            String roleName = userRole.getRole().getName();
                            return "ADMIN".equals(roleName) || "MODERATOR".equals(roleName);
                        }))
                .forEach(moderator -> notificationService.createNotification(
                        moderator,
                        reporter,
                        NotificationType.REPORT_UPDATE,
                        "New report",
                        reporter.getUsername() + " submitted a new report.",
                        "REPORT",
                        report.getId()
                ));
    }

    private ReportTargetResponse userTarget(Report report) {
        User user = userRepository.findById(report.getTargetId())
                .orElseThrow(() -> new ResourceNotFoundException("Report target not found."));
        Profile profile = user.getProfile();

        return new ReportTargetResponse(
                report.getId(),
                report.getTargetType(),
                report.getTargetId(),
                UserMapper.toSummary(user),
                user.getEmail(),
                user.getStatus(),
                profile != null ? profile.getBio() : null,
                null,
                null,
                null,
                null,
                null,
                null,
                user.getStatus().name(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                user.getCreatedAt(),
                List.of()
        );
    }

    private ReportTargetResponse postTarget(Report report) {
        Post post = postRepository.findById(report.getTargetId())
                .orElseThrow(() -> new ResourceNotFoundException("Report target not found."));
        List<ReportTargetResponse.MediaPreviewResponse> media = mediaRepository.findByPostId(post.getId())
                .stream()
                .map(this::toMediaPreview)
                .toList();

        return new ReportTargetResponse(
                report.getId(),
                report.getTargetType(),
                report.getTargetId(),
                null,
                null,
                null,
                null,
                UserMapper.toSummary(post.getAuthor()),
                null,
                null,
                null,
                post.getId(),
                post.getContent(),
                post.getStatus().name(),
                post.getVisibility().name(),
                post.getCommentCount(),
                post.getReactionCount(),
                null,
                null,
                null,
                null,
                null,
                post.getCreatedAt(),
                media
        );
    }

    private ReportTargetResponse commentTarget(Report report) {
        Comment comment = commentRepository.findById(report.getTargetId())
                .orElseThrow(() -> new ResourceNotFoundException("Report target not found."));

        return new ReportTargetResponse(
                report.getId(),
                report.getTargetType(),
                report.getTargetId(),
                null,
                null,
                null,
                null,
                UserMapper.toSummary(comment.getAuthor()),
                null,
                null,
                null,
                comment.getPost().getId(),
                comment.getContent(),
                comment.getStatus().name(),
                null,
                null,
                comment.getReactionCount(),
                null,
                null,
                null,
                null,
                null,
                comment.getCreatedAt(),
                List.of()
        );
    }

    private ReportTargetResponse messageTarget(Report report) {
        Message message = messageRepository.findById(report.getTargetId())
                .orElseThrow(() -> new ResourceNotFoundException("Report target not found."));

        return new ReportTargetResponse(
                report.getId(),
                report.getTargetType(),
                report.getTargetId(),
                null,
                null,
                null,
                null,
                null,
                UserMapper.toSummary(message.getSender()),
                UserMapper.toSummary(message.getRecipient()),
                null,
                null,
                message.getContent(),
                message.getStatus().name(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                message.getSentAt(),
                List.of()
        );
    }

    private ReportTargetResponse mediaTarget(Report report) {
        Media media = mediaRepository.findById(report.getTargetId())
                .orElseThrow(() -> new ResourceNotFoundException("Report target not found."));

        return new ReportTargetResponse(
                report.getId(),
                report.getTargetType(),
                report.getTargetId(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                UserMapper.toSummary(media.getUploader()),
                media.getPost() != null ? media.getPost().getId() : null,
                null,
                media.getStatus().name(),
                null,
                null,
                null,
                media.getUrl(),
                media.getMediaType(),
                media.getMimeType(),
                media.getSizeBytes(),
                media.getAltText(),
                media.getCreatedAt(),
                List.of()
        );
    }

    private ReportTargetResponse.MediaPreviewResponse toMediaPreview(Media media) {
        return new ReportTargetResponse.MediaPreviewResponse(
                media.getId(),
                media.getUrl(),
                media.getMediaType(),
                media.getMimeType(),
                media.getSizeBytes(),
                media.getAltText()
        );
    }

    private void validateReportableTarget(ReportTargetType targetType, UUID targetId, User reporter) {
        switch (targetType) {
            case USER -> {
                if (!userRepository.existsById(targetId)) {
                    throw new ResourceNotFoundException("Report target not found.");
                }
            }
            case POST -> {
                Post post = postRepository.findById(targetId)
                        .orElseThrow(() -> new ResourceNotFoundException("Report target not found."));
                if (post.getStatus() != PostStatus.PUBLISHED) {
                    throw new ResourceNotFoundException("Report target not found.");
                }
            }
            case COMMENT -> commentRepository.findById(targetId)
                    .filter(comment -> comment.getStatus() == CommentStatus.VISIBLE)
                    .orElseThrow(() -> new ResourceNotFoundException("Report target not found."));
            case MESSAGE -> {
                Message message = messageRepository.findById(targetId)
                        .orElseThrow(() -> new ResourceNotFoundException("Report target not found."));
                if (message.getStatus() == com.tinsae.socialmediaplatform.common.enums.MessageStatus.DELETED) {
                    throw new ResourceNotFoundException("Report target not found.");
                }
                if (!message.getSender().getId().equals(reporter.getId())
                        && !message.getRecipient().getId().equals(reporter.getId())) {
                    throw new UnauthorizedActionException("You can only report messages you sent or received.");
                }
            }
            case MEDIA -> {
                if (!mediaRepository.existsById(targetId)) {
                    throw new ResourceNotFoundException("Report target not found.");
                }
            }
        }
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(limit, MAX_LIMIT));
    }
}
