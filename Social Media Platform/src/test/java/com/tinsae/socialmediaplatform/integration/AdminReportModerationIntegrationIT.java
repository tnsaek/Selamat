package com.tinsae.socialmediaplatform.integration;

import com.tinsae.socialmediaplatform.admin.repository.ModerationAuditLogRepository;
import com.tinsae.socialmediaplatform.common.enums.NotificationType;
import com.tinsae.socialmediaplatform.common.enums.PostStatus;
import com.tinsae.socialmediaplatform.common.enums.ReportStatus;
import com.tinsae.socialmediaplatform.common.enums.ReportTargetType;
import com.tinsae.socialmediaplatform.notification.repository.NotificationRepository;
import com.tinsae.socialmediaplatform.post.entity.Post;
import com.tinsae.socialmediaplatform.post.repository.PostRepository;
import com.tinsae.socialmediaplatform.report.entity.Report;
import com.tinsae.socialmediaplatform.report.repository.ReportRepository;
import com.tinsae.socialmediaplatform.role.entity.Role;
import com.tinsae.socialmediaplatform.role.entity.UserRole;
import com.tinsae.socialmediaplatform.role.entity.UserRoleId;
import com.tinsae.socialmediaplatform.role.repository.RoleRepository;
import com.tinsae.socialmediaplatform.role.repository.UserRoleRepository;
import com.tinsae.socialmediaplatform.user.entity.User;
import com.tinsae.socialmediaplatform.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.http.HttpResponse;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AdminReportModerationIntegrationIT extends AbstractIntegrationTest {

    @Autowired
    private ModerationAuditLogRepository moderationAuditLogRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Test
    void adminReviewsResolvesReportAndHidesReportedPost() throws Exception {
        String authorUsername = uniqueUsername("author");
        String reporterUsername = uniqueUsername("reporter");
        String adminUsername = uniqueUsername("admin");

        String authorToken = extractAccessToken(signUp(authorUsername, authorUsername + "@example.com").body());
        String reporterToken = extractAccessToken(signUp(reporterUsername, reporterUsername + "@example.com").body());
        signUp(adminUsername, adminUsername + "@example.com");
        User admin = promoteToAdmin(adminUsername);
        String adminToken = extractAccessToken(login(admin.getEmail(), "StrongPassword123").body());

        String content = "Reported integration post " + UUID.randomUUID();
        HttpResponse<String> createPostResponse = postJson("/api/posts", """
                {
                  "content": "%s",
                  "visibility": "PUBLIC",
                  "mediaIds": []
                }
                """.formatted(content), authorToken);
        assertThat(createPostResponse.statusCode()).isEqualTo(201);

        Post post = postRepository.findAll()
                .stream()
                .filter(candidate -> content.equals(candidate.getContent()))
                .findFirst()
                .orElseThrow();

        HttpResponse<String> createReportResponse = postJson("/api/reports", """
                {
                  "targetType": "POST",
                  "targetId": "%s",
                  "reason": "Spam",
                  "details": "This post was reported by an integration test."
                }
                """.formatted(post.getId()), reporterToken);

        assertThat(createReportResponse.statusCode()).isEqualTo(201);
        assertThat(createReportResponse.body())
                .contains("\"targetType\":\"POST\"")
                .contains("\"targetId\":\"" + post.getId() + "\"")
                .contains("\"status\":\"OPEN\"");

        Report report = reportRepository.findAll()
                .stream()
                .filter(candidate -> post.getId().equals(candidate.getTargetId()))
                .findFirst()
                .orElseThrow();
        assertThat(report.getStatus()).isEqualTo(ReportStatus.OPEN);

        assertThat(notificationRepository.findAll())
                .anySatisfy(notification -> {
                    assertThat(notification.getRecipient().getId()).isEqualTo(admin.getId());
                    assertThat(notification.getNotificationType()).isEqualTo(NotificationType.REPORT_UPDATE);
                    assertThat(notification.getTargetType()).isEqualTo("REPORT");
                    assertThat(notification.getTargetId()).isEqualTo(report.getId());
                    assertThat(notification.getRead()).isFalse();
                });

        HttpResponse<String> listReportsResponse = get("/api/admin/reports?status=OPEN&limit=10", adminToken);

        assertThat(listReportsResponse.statusCode()).isEqualTo(200);
        assertThat(listReportsResponse.body())
                .contains("\"items\"")
                .contains("\"id\":\"" + report.getId() + "\"");

        HttpResponse<String> summaryResponse = get("/api/admin/reports/summary", adminToken);

        assertThat(summaryResponse.statusCode()).isEqualTo(200);
        assertThat(summaryResponse.body()).contains("\"openReports\":1");

        HttpResponse<String> targetResponse = get("/api/admin/reports/%s/target".formatted(report.getId()), adminToken);

        assertThat(targetResponse.statusCode()).isEqualTo(200);
        assertThat(targetResponse.body())
                .contains("\"targetType\":\"POST\"")
                .contains("\"postId\":\"" + post.getId() + "\"")
                .contains("\"content\":\"" + content + "\"");

        HttpResponse<String> resolveResponse = patchJson("/api/admin/reports/%s/resolve".formatted(report.getId()), """
                {
                  "status": "RESOLVED",
                  "resolutionNote": "Post hidden by moderation."
                }
                """, adminToken);

        assertThat(resolveResponse.statusCode()).isEqualTo(200);
        assertThat(resolveResponse.body())
                .contains("\"status\":\"RESOLVED\"")
                .contains("\"resolutionNote\":\"Post hidden by moderation.\"");

        Report resolvedReport = reportRepository.findById(report.getId()).orElseThrow();
        assertThat(resolvedReport.getStatus()).isEqualTo(ReportStatus.RESOLVED);
        assertThat(resolvedReport.getResolver().getId()).isEqualTo(admin.getId());
        assertThat(resolvedReport.getResolvedAt()).isNotNull();

        HttpResponse<String> hidePostResponse = patchJson(
                "/api/admin/moderation/posts/%s/hide".formatted(post.getId()),
                "{}",
                adminToken
        );

        assertThat(hidePostResponse.statusCode()).isEqualTo(204);
        assertThat(postRepository.findById(post.getId()).orElseThrow().getStatus()).isEqualTo(PostStatus.HIDDEN);
        assertThat(moderationAuditLogRepository.findAll())
                .anySatisfy(auditLog -> {
                    assertThat(auditLog.getActor().getId()).isEqualTo(admin.getId());
                    assertThat(auditLog.getAction()).isEqualTo("HIDE_POST");
                    assertThat(auditLog.getTargetType()).isEqualTo(ReportTargetType.POST);
                    assertThat(auditLog.getTargetId()).isEqualTo(post.getId());
                });
    }

    private User promoteToAdmin(String username) {
        User admin = userRepository.findByUsername(username).orElseThrow();
        Role role = roleRepository.findByName("ADMIN").orElseGet(() -> {
            Role newRole = new Role();
            newRole.setName("ADMIN");
            newRole.setDescription("Administrator");
            return roleRepository.save(newRole);
        });

        UserRoleId userRoleId = new UserRoleId(admin.getId(), role.getId());
        if (!userRoleRepository.existsById(userRoleId)) {
            UserRole userRole = new UserRole();
            userRole.setId(userRoleId);
            userRole.setUser(admin);
            userRole.setRole(role);
            userRoleRepository.save(userRole);
        }

        return admin;
    }

    private String uniqueUsername(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }
}
