package com.tinsae.socialmediaplatform.report.service;

import com.tinsae.socialmediaplatform.TestDataFactory;
import com.tinsae.socialmediaplatform.comment.entity.Comment;
import com.tinsae.socialmediaplatform.comment.repository.CommentRepository;
import com.tinsae.socialmediaplatform.common.enums.CommentStatus;
import com.tinsae.socialmediaplatform.common.enums.MessageStatus;
import com.tinsae.socialmediaplatform.common.enums.NotificationType;
import com.tinsae.socialmediaplatform.common.enums.PostStatus;
import com.tinsae.socialmediaplatform.common.enums.ReportStatus;
import com.tinsae.socialmediaplatform.common.enums.ReportTargetType;
import com.tinsae.socialmediaplatform.common.exception.BusinessRuleException;
import com.tinsae.socialmediaplatform.common.exception.ResourceNotFoundException;
import com.tinsae.socialmediaplatform.common.exception.UnauthorizedActionException;
import com.tinsae.socialmediaplatform.media.repository.MediaRepository;
import com.tinsae.socialmediaplatform.message.entity.Message;
import com.tinsae.socialmediaplatform.message.repository.MessageRepository;
import com.tinsae.socialmediaplatform.notification.service.NotificationService;
import com.tinsae.socialmediaplatform.post.entity.Post;
import com.tinsae.socialmediaplatform.post.repository.PostRepository;
import com.tinsae.socialmediaplatform.report.dto.CreateReportRequest;
import com.tinsae.socialmediaplatform.report.dto.ResolveReportRequest;
import com.tinsae.socialmediaplatform.report.entity.Report;
import com.tinsae.socialmediaplatform.report.repository.ReportRepository;
import com.tinsae.socialmediaplatform.user.entity.User;
import com.tinsae.socialmediaplatform.user.repository.UserRepository;
import com.tinsae.socialmediaplatform.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private PostRepository postRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private ReportService reportService;

    @Test
    void createReportSavesOpenReport() {
        User reporter = TestDataFactory.user("reporter");
        Post post = TestDataFactory.post(TestDataFactory.user("author"));
        var jwt = TestDataFactory.jwt(reporter);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(reporter);
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
        when(reportRepository.existsByReporterIdAndTargetTypeAndTargetIdAndStatus(
                reporter.getId(),
                ReportTargetType.POST,
                post.getId(),
                ReportStatus.OPEN
        )).thenReturn(false);
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> {
            Report report = invocation.getArgument(0);
            report.setId(UUID.randomUUID());
            return report;
        });

        var response = reportService.createReport(
                new CreateReportRequest(ReportTargetType.POST, post.getId(), "Spam", "Details"),
                jwt
        );

        assertThat(response.status()).isEqualTo(ReportStatus.OPEN);
        assertThat(response.targetId()).isEqualTo(post.getId());
    }

    @Test
    void createReportRejectsSelfReport() {
        User reporter = TestDataFactory.user("reporter");
        var jwt = TestDataFactory.jwt(reporter);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(reporter);
        when(userRepository.existsById(reporter.getId())).thenReturn(true);

        assertThatThrownBy(() -> reportService.createReport(
                new CreateReportRequest(ReportTargetType.USER, reporter.getId(), "Reason", null),
                jwt
        )).isInstanceOf(BusinessRuleException.class)
                .hasMessage("You cannot report yourself.");
    }

    @Test
    void createReportNotifiesAdminsAndModeratorsOnly() {
        User reporter = TestDataFactory.user("reporter");
        User admin = TestDataFactory.user("admin");
        User moderator = TestDataFactory.user("moderator");
        User regularUser = TestDataFactory.user("regular");
        TestDataFactory.userRole(admin, TestDataFactory.role("ADMIN"));
        TestDataFactory.userRole(moderator, TestDataFactory.role("MODERATOR"));
        TestDataFactory.userRole(regularUser, TestDataFactory.role("USER"));
        Post post = TestDataFactory.post(TestDataFactory.user("author"));
        var jwt = TestDataFactory.jwt(reporter);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(reporter);
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
        when(reportRepository.existsByReporterIdAndTargetTypeAndTargetIdAndStatus(
                reporter.getId(),
                ReportTargetType.POST,
                post.getId(),
                ReportStatus.OPEN
        )).thenReturn(false);
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> {
            Report report = invocation.getArgument(0);
            report.setId(UUID.randomUUID());
            return report;
        });
        when(userRepository.findAll()).thenReturn(List.of(admin, moderator, regularUser));

        var response = reportService.createReport(
                new CreateReportRequest(ReportTargetType.POST, post.getId(), "Spam", "Details"),
                jwt
        );

        verify(notificationService).createNotification(
                eq(admin),
                eq(reporter),
                eq(NotificationType.REPORT_UPDATE),
                eq("New report"),
                eq("reporter submitted a new report."),
                eq("REPORT"),
                eq(response.id())
        );
        verify(notificationService).createNotification(
                eq(moderator),
                eq(reporter),
                eq(NotificationType.REPORT_UPDATE),
                eq("New report"),
                eq("reporter submitted a new report."),
                eq("REPORT"),
                eq(response.id())
        );
    }

    @Test
    void createReportRejectsMissingTarget() {
        User reporter = TestDataFactory.user("reporter");
        UUID targetId = UUID.randomUUID();
        var jwt = TestDataFactory.jwt(reporter);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(reporter);
        when(postRepository.findById(targetId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.createReport(
                new CreateReportRequest(ReportTargetType.POST, targetId, "Reason", null),
                jwt
        )).isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Report target not found.");
    }

    @Test
    void createReportRejectsMissingUserTarget() {
        User reporter = TestDataFactory.user("reporter");
        UUID targetId = UUID.randomUUID();
        var jwt = TestDataFactory.jwt(reporter);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(reporter);
        when(userRepository.existsById(targetId)).thenReturn(false);

        assertThatThrownBy(() -> reportService.createReport(
                new CreateReportRequest(ReportTargetType.USER, targetId, "Reason", null),
                jwt
        )).isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Report target not found.");
    }

    @Test
    void createReportRejectsDuplicateOpenReport() {
        User reporter = TestDataFactory.user("reporter");
        Post post = TestDataFactory.post(TestDataFactory.user("author"));
        Comment comment = TestDataFactory.comment(post, TestDataFactory.user("commenter"));
        UUID targetId = comment.getId();
        var jwt = TestDataFactory.jwt(reporter);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(reporter);
        when(commentRepository.findById(targetId)).thenReturn(Optional.of(comment));
        when(reportRepository.existsByReporterIdAndTargetTypeAndTargetIdAndStatus(
                reporter.getId(),
                ReportTargetType.COMMENT,
                targetId,
                ReportStatus.OPEN
        )).thenReturn(true);

        assertThatThrownBy(() -> reportService.createReport(
                new CreateReportRequest(ReportTargetType.COMMENT, targetId, "Reason", null),
                jwt
        )).isInstanceOf(BusinessRuleException.class)
                .hasMessage("You already have an open report for this target.");
    }

    @Test
    void createReportRejectsDeletedPostTarget() {
        User reporter = TestDataFactory.user("reporter");
        Post post = TestDataFactory.post(TestDataFactory.user("author"));
        post.setStatus(PostStatus.DELETED);
        var jwt = TestDataFactory.jwt(reporter);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(reporter);
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> reportService.createReport(
                new CreateReportRequest(ReportTargetType.POST, post.getId(), "Reason", null),
                jwt
        )).isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Report target not found.");
    }

    @Test
    void createReportRejectsHiddenPostTarget() {
        User reporter = TestDataFactory.user("reporter");
        Post post = TestDataFactory.post(TestDataFactory.user("author"));
        post.setStatus(PostStatus.HIDDEN);
        var jwt = TestDataFactory.jwt(reporter);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(reporter);
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> reportService.createReport(
                new CreateReportRequest(ReportTargetType.POST, post.getId(), "Reason", null),
                jwt
        )).isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Report target not found.");
    }

    @Test
    void createReportRejectsHiddenCommentTarget() {
        User reporter = TestDataFactory.user("reporter");
        Post post = TestDataFactory.post(TestDataFactory.user("author"));
        Comment comment = TestDataFactory.comment(post, TestDataFactory.user("commenter"));
        comment.setStatus(CommentStatus.HIDDEN);
        var jwt = TestDataFactory.jwt(reporter);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(reporter);
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> reportService.createReport(
                new CreateReportRequest(ReportTargetType.COMMENT, comment.getId(), "Reason", null),
                jwt
        )).isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Report target not found.");
    }

    @Test
    void createReportRejectsMissingMessageTarget() {
        User reporter = TestDataFactory.user("reporter");
        UUID messageId = UUID.randomUUID();
        var jwt = TestDataFactory.jwt(reporter);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(reporter);
        when(messageRepository.findById(messageId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.createReport(
                new CreateReportRequest(ReportTargetType.MESSAGE, messageId, "Reason", null),
                jwt
        )).isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Report target not found.");
    }

    @Test
    void createReportRejectsDeletedMessageTarget() {
        User reporter = TestDataFactory.user("reporter");
        Message message = TestDataFactory.message(reporter, TestDataFactory.user("recipient"));
        message.setStatus(MessageStatus.DELETED);
        var jwt = TestDataFactory.jwt(reporter);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(reporter);
        when(messageRepository.findById(message.getId())).thenReturn(Optional.of(message));

        assertThatThrownBy(() -> reportService.createReport(
                new CreateReportRequest(ReportTargetType.MESSAGE, message.getId(), "Reason", null),
                jwt
        )).isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Report target not found.");
    }

    @Test
    void createReportRejectsMissingMediaTarget() {
        User reporter = TestDataFactory.user("reporter");
        UUID mediaId = UUID.randomUUID();
        var jwt = TestDataFactory.jwt(reporter);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(reporter);
        when(mediaRepository.existsById(mediaId)).thenReturn(false);

        assertThatThrownBy(() -> reportService.createReport(
                new CreateReportRequest(ReportTargetType.MEDIA, mediaId, "Reason", null),
                jwt
        )).isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Report target not found.");
    }

    @Test
    void createReportRejectsMessageTargetWhenReporterIsNotParticipant() {
        User reporter = TestDataFactory.user("reporter");
        Message message = TestDataFactory.message(TestDataFactory.user("sender"), TestDataFactory.user("recipient"));
        var jwt = TestDataFactory.jwt(reporter);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(reporter);
        when(messageRepository.findById(message.getId())).thenReturn(Optional.of(message));

        assertThatThrownBy(() -> reportService.createReport(
                new CreateReportRequest(ReportTargetType.MESSAGE, message.getId(), "Reason", null),
                jwt
        )).isInstanceOf(UnauthorizedActionException.class)
                .hasMessage("You can only report messages you sent or received.");
    }

    @Test
    void createReportValidatesAllTargetTypes() {
        assertReportCreatedForTarget(ReportTargetType.USER);
        assertReportCreatedForTarget(ReportTargetType.COMMENT);
        assertReportCreatedForTarget(ReportTargetType.MESSAGE);
        assertReportCreatedForTarget(ReportTargetType.MEDIA);
    }

    @Test
    void createReportAllowsMessageTargetWhenReporterIsSender() {
        User reporter = TestDataFactory.user("reporter");
        Message message = TestDataFactory.message(reporter, TestDataFactory.user("recipient"));
        var jwt = TestDataFactory.jwt(reporter);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(reporter);
        when(messageRepository.findById(message.getId())).thenReturn(Optional.of(message));
        when(reportRepository.existsByReporterIdAndTargetTypeAndTargetIdAndStatus(
                reporter.getId(),
                ReportTargetType.MESSAGE,
                message.getId(),
                ReportStatus.OPEN
        )).thenReturn(false);
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> {
            Report report = invocation.getArgument(0);
            report.setId(UUID.randomUUID());
            return report;
        });

        var response = reportService.createReport(
                new CreateReportRequest(ReportTargetType.MESSAGE, message.getId(), "Reason", null),
                jwt
        );

        assertThat(response.targetType()).isEqualTo(ReportTargetType.MESSAGE);
        assertThat(response.targetId()).isEqualTo(message.getId());
    }

    @Test
    void listReportsReturnsAllReportsWhenStatusIsNullAndUsesDefaultLimit() {
        Report report = TestDataFactory.report(TestDataFactory.user("reporter"), UUID.randomUUID());
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(reportRepository.findAdminReports(
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                pageableCaptor.capture()
        )).thenReturn(List.of(report));

        var response = reportService.listReports(null, null, null);

        assertThat(response.items()).hasSize(1);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(21);
    }

    @Test
    void listReportsFiltersByStatusAndClampsLowLimit() {
        Report report = TestDataFactory.report(TestDataFactory.user("reporter"), UUID.randomUUID());
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(reportRepository.findAdminReports(
                org.mockito.ArgumentMatchers.eq(ReportStatus.OPEN),
                org.mockito.ArgumentMatchers.isNull(),
                pageableCaptor.capture()
        )).thenReturn(List.of(report));

        var response = reportService.listReports(ReportStatus.OPEN, null, 0);

        assertThat(response.items()).hasSize(1);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(2);
    }

    @Test
    void listReportsClampsHighLimit() {
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(reportRepository.findAdminReports(
                org.mockito.ArgumentMatchers.eq(ReportStatus.RESOLVED),
                org.mockito.ArgumentMatchers.isNull(),
                pageableCaptor.capture()
        )).thenReturn(List.of());

        reportService.listReports(ReportStatus.RESOLVED, null, 101);

        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(101);
    }

    @Test
    void getAdminSummaryCountsStatusesAndTargetTypes() {
        when(reportRepository.countByStatus(ReportStatus.OPEN)).thenReturn(2L);
        when(reportRepository.countByStatus(ReportStatus.UNDER_REVIEW)).thenReturn(1L);
        when(reportRepository.countByStatus(ReportStatus.RESOLVED)).thenReturn(4L);
        when(reportRepository.countByStatus(ReportStatus.REJECTED)).thenReturn(3L);
        when(reportRepository.countByTargetType(ReportTargetType.USER)).thenReturn(5L);
        when(reportRepository.countByTargetType(ReportTargetType.POST)).thenReturn(6L);
        when(reportRepository.countByTargetType(ReportTargetType.COMMENT)).thenReturn(7L);
        when(reportRepository.countByTargetType(ReportTargetType.MESSAGE)).thenReturn(8L);
        when(reportRepository.countByTargetType(ReportTargetType.MEDIA)).thenReturn(9L);

        var response = reportService.getAdminSummary();

        assertThat(response.openReports()).isEqualTo(2L);
        assertThat(response.underReviewReports()).isEqualTo(1L);
        assertThat(response.resolvedReports()).isEqualTo(4L);
        assertThat(response.rejectedReports()).isEqualTo(3L);
        assertThat(response.userReports()).isEqualTo(5L);
        assertThat(response.contentReports()).isEqualTo(21L);
        assertThat(response.mediaReports()).isEqualTo(9L);
    }

    @Test
    void listReportsPassesParsedCursorAndReturnsNextCursor() {
        Report first = TestDataFactory.report(TestDataFactory.user("reporter-one"), UUID.randomUUID());
        Report extra = TestDataFactory.report(TestDataFactory.user("reporter-two"), UUID.randomUUID());
        first.setCreatedAt(Instant.parse("2026-07-16T10:15:30Z"));
        extra.setCreatedAt(Instant.parse("2026-07-15T10:15:30Z"));
        ArgumentCaptor<Instant> cursorCaptor = ArgumentCaptor.forClass(Instant.class);
        when(reportRepository.findAdminReports(
                org.mockito.ArgumentMatchers.eq(ReportStatus.OPEN),
                cursorCaptor.capture(),
                any(Pageable.class)
        )).thenReturn(List.of(first, extra));

        var response = reportService.listReports(ReportStatus.OPEN, "2026-07-17T10:15:30Z", 1);

        assertThat(cursorCaptor.getValue()).isEqualTo(Instant.parse("2026-07-17T10:15:30Z"));
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().id()).isEqualTo(first.getId());
        assertThat(response.nextCursor()).isEqualTo("2026-07-16T10:15:30Z");
    }

    @Test
    void listReportsRejectsInvalidCursor() {
        assertThatThrownBy(() -> reportService.listReports(null, "not-a-date", 10))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Invalid cursor format. Use ISO-8601 format.");
    }

    @Test
    void getReportByIdReturnsReport() {
        Report report = TestDataFactory.report(TestDataFactory.user("reporter"), UUID.randomUUID());
        when(reportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        var response = reportService.getReportById(report.getId());

        assertThat(response.id()).isEqualTo(report.getId());
        assertThat(response.targetId()).isEqualTo(report.getTargetId());
    }

    @Test
    void getReportByIdRejectsMissingReport() {
        UUID reportId = UUID.randomUUID();
        when(reportRepository.findById(reportId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.getReportById(reportId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Report not found.");
    }

    @Test
    void resolveReportSetsResolverStatusAndResolvedAt() {
        User moderator = TestDataFactory.user("moderator");
        Report report = TestDataFactory.report(TestDataFactory.user("reporter"), UUID.randomUUID());
        var jwt = TestDataFactory.jwt(moderator);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(moderator);
        when(reportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(reportRepository.save(report)).thenReturn(report);

        var response = reportService.resolveReport(
                report.getId(),
                new ResolveReportRequest(ReportStatus.RESOLVED, "Done"),
                jwt
        );

        assertThat(response.status()).isEqualTo(ReportStatus.RESOLVED);
        assertThat(response.resolutionNote()).isEqualTo("Done");
        assertThat(response.resolver().id()).isEqualTo(moderator.getId());
        assertThat(response.resolvedAt()).isNotNull();
    }

    @Test
    void resolveReportRejectsMissingReport() {
        User moderator = TestDataFactory.user("moderator");
        UUID reportId = UUID.randomUUID();
        var jwt = TestDataFactory.jwt(moderator);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(moderator);
        when(reportRepository.findById(reportId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.resolveReport(
                reportId,
                new ResolveReportRequest(ReportStatus.REJECTED, "No issue"),
                jwt
        )).isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Report not found.");
    }

    @Test
    void resolveReportRejectsOpenStatus() {
        User moderator = TestDataFactory.user("moderator");
        Report report = TestDataFactory.report(TestDataFactory.user("reporter"), UUID.randomUUID());
        var jwt = TestDataFactory.jwt(moderator);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(moderator);
        when(reportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> reportService.resolveReport(
                report.getId(),
                new ResolveReportRequest(ReportStatus.OPEN, "Still open"),
                jwt
        )).isInstanceOf(BusinessRuleException.class)
                .hasMessage("Report can only be resolved as RESOLVED or REJECTED.");
    }

    @Test
    void resolveReportRejectsUnderReviewStatus() {
        User moderator = TestDataFactory.user("moderator");
        Report report = TestDataFactory.report(TestDataFactory.user("reporter"), UUID.randomUUID());
        var jwt = TestDataFactory.jwt(moderator);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(moderator);
        when(reportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> reportService.resolveReport(
                report.getId(),
                new ResolveReportRequest(ReportStatus.UNDER_REVIEW, "Reviewing"),
                jwt
        )).isInstanceOf(BusinessRuleException.class)
                .hasMessage("Report can only be resolved as RESOLVED or REJECTED.");
    }

    @Test
    void resolveReportRejectsAlreadyResolvedReport() {
        User moderator = TestDataFactory.user("moderator");
        Report report = TestDataFactory.report(TestDataFactory.user("reporter"), UUID.randomUUID());
        report.setStatus(ReportStatus.RESOLVED);
        var jwt = TestDataFactory.jwt(moderator);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(moderator);
        when(reportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> reportService.resolveReport(
                report.getId(),
                new ResolveReportRequest(ReportStatus.REJECTED, "No issue"),
                jwt
        )).isInstanceOf(BusinessRuleException.class)
                .hasMessage("Report has already been resolved.");
    }

    @Test
    void resolveReportRejectsAlreadyRejectedReport() {
        User moderator = TestDataFactory.user("moderator");
        Report report = TestDataFactory.report(TestDataFactory.user("reporter"), UUID.randomUUID());
        report.setStatus(ReportStatus.REJECTED);
        var jwt = TestDataFactory.jwt(moderator);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(moderator);
        when(reportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> reportService.resolveReport(
                report.getId(),
                new ResolveReportRequest(ReportStatus.RESOLVED, "Resolved"),
                jwt
        )).isInstanceOf(BusinessRuleException.class)
                .hasMessage("Report has already been resolved.");
    }

    private void assertReportCreatedForTarget(ReportTargetType targetType) {
        User reporter = TestDataFactory.user("reporter-" + targetType.name().toLowerCase());
        UUID targetId = stubReportableTarget(targetType, reporter);
        var jwt = TestDataFactory.jwt(reporter);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(reporter);
        when(reportRepository.existsByReporterIdAndTargetTypeAndTargetIdAndStatus(
                reporter.getId(),
                targetType,
                targetId,
                ReportStatus.OPEN
        )).thenReturn(false);
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> {
            Report report = invocation.getArgument(0);
            report.setId(UUID.randomUUID());
            return report;
        });

        var response = reportService.createReport(
                new CreateReportRequest(targetType, targetId, "Reason", null),
                jwt
        );

        assertThat(response.targetType()).isEqualTo(targetType);
        assertThat(response.targetId()).isEqualTo(targetId);
    }

    private UUID stubReportableTarget(ReportTargetType targetType, User reporter) {
        return switch (targetType) {
            case USER -> {
                User target = TestDataFactory.user("reported-user");
                when(userRepository.existsById(target.getId())).thenReturn(true);
                yield target.getId();
            }
            case POST -> {
                Post post = TestDataFactory.post(TestDataFactory.user("author"));
                when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
                yield post.getId();
            }
            case COMMENT -> {
                Post post = TestDataFactory.post(TestDataFactory.user("author"));
                Comment comment = TestDataFactory.comment(post, TestDataFactory.user("commenter"));
                when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
                yield comment.getId();
            }
            case MESSAGE -> {
                Message message = TestDataFactory.message(TestDataFactory.user("sender"), reporter);
                when(messageRepository.findById(message.getId())).thenReturn(Optional.of(message));
                yield message.getId();
            }
            case MEDIA -> {
                UUID mediaId = UUID.randomUUID();
                when(mediaRepository.existsById(mediaId)).thenReturn(true);
                yield mediaId;
            }
        };
    }
}
