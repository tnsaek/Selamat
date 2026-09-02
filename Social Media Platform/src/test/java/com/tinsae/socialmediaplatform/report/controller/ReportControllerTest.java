package com.tinsae.socialmediaplatform.report.controller;

import com.tinsae.socialmediaplatform.TestDataFactory;
import com.tinsae.socialmediaplatform.common.enums.ReportStatus;
import com.tinsae.socialmediaplatform.common.enums.ReportTargetType;
import com.tinsae.socialmediaplatform.report.dto.CreateReportRequest;
import com.tinsae.socialmediaplatform.report.dto.ReportResponse;
import com.tinsae.socialmediaplatform.report.service.ReportService;
import com.tinsae.socialmediaplatform.user.dto.UserSummaryResponse;
import com.tinsae.socialmediaplatform.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    @Mock
    private ReportService reportService;

    @InjectMocks
    private ReportController reportController;

    @Test
    void createReportReturnsCreatedResponse() {
        User reporter = TestDataFactory.user("reporter");
        var jwt = TestDataFactory.jwt(reporter);
        UUID targetId = UUID.randomUUID();
        CreateReportRequest request = new CreateReportRequest(ReportTargetType.POST, targetId, "Spam", "Details");
        ReportResponse report = new ReportResponse(
                UUID.randomUUID(),
                new UserSummaryResponse(reporter.getId(), reporter.getUsername(), null, null),
                null,
                ReportTargetType.POST,
                targetId,
                "Spam",
                "Details",
                null,
                ReportStatus.OPEN,
                Instant.now(),
                null
        );
        when(reportService.createReport(request, jwt)).thenReturn(report);

        var response = reportController.createReport(request, jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(report);
        verify(reportService).createReport(request, jwt);
    }
}
