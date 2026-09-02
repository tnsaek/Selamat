import { DatePipe, TitleCasePipe } from '@angular/common';
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize, Observable } from 'rxjs';

import { ModerationService, ReportService } from '../../core/services';
import { ErrorResponse, Report, ReportStatus, ReportTarget, UserSummary, UUID } from '../../models';

const REPORT_STATUSES: Array<ReportStatus | 'ALL'> = ['ALL', 'OPEN', 'UNDER_REVIEW', 'RESOLVED', 'REJECTED'];

@Component({
  selector: 'app-admin-reports-page',
  imports: [DatePipe, TitleCasePipe],
  templateUrl: './admin-reports-page.html',
  styleUrl: './admin-reports-page.scss',
})
export class AdminReportsPage implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly moderationService = inject(ModerationService);
  private readonly reportService = inject(ReportService);
  private readonly route = inject(ActivatedRoute);
  private readonly pageSize = 20;

  readonly statuses = REPORT_STATUSES;
  readonly reports = signal<Report[]>([]);
  readonly selectedStatus = signal<ReportStatus | 'ALL'>('ALL');
  readonly targetReportId = signal<UUID | null>(null);
  readonly nextCursor = signal<string | null>(null);
  readonly isLoading = signal(true);
  readonly isLoadingMore = signal(false);
  readonly isLoadingTargetReport = signal(false);
  readonly loadingTargetIds = signal<Record<UUID, boolean>>({});
  readonly moderatingTargetIds = signal<Record<UUID, boolean>>({});
  readonly resolvingReportIds = signal<Record<UUID, boolean>>({});
  readonly resolutionNoteByReportId = signal<Record<UUID, string>>({});
  readonly targetByReportId = signal<Record<UUID, ReportTarget>>({});
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  ngOnInit(): void {
    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      this.targetReportId.set(params.get('reportId'));

      if (!this.isLoading()) {
        this.ensureTargetReportVisible();
      }
    });

    this.loadReports();
  }

  loadReports(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.reportService
      .listReports({
        status: this.selectedReportStatus(),
        limit: this.pageSize,
      })
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (response) => {
          this.reports.set(response.items);
          this.nextCursor.set(response.nextCursor);
          this.ensureTargetReportVisible();
        },
        error: (error) => this.errorMessage.set(this.errorText(error)),
      });
  }

  loadMore(): void {
    const cursor = this.nextCursor();
    if (!cursor || this.isLoadingMore()) {
      return;
    }

    this.isLoadingMore.set(true);
    this.errorMessage.set(null);

    this.reportService
      .listReports({
        status: this.selectedReportStatus(),
        cursor,
        limit: this.pageSize,
      })
      .pipe(finalize(() => this.isLoadingMore.set(false)))
      .subscribe({
        next: (response) => {
          this.reports.update((reports) => [...reports, ...response.items]);
          this.nextCursor.set(response.nextCursor);
          this.ensureTargetReportVisible();
        },
        error: (error) => this.errorMessage.set(this.errorText(error)),
      });
  }

  changeStatus(event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.selectedStatus.set(select.value as ReportStatus | 'ALL');
    this.loadReports();
  }

  resolveReport(report: Report, status: Extract<ReportStatus, 'RESOLVED' | 'REJECTED'>): void {
    if (this.resolvingReportIds()[report.id] || this.isFinalStatus(report.status)) {
      return;
    }

    this.setResolving(report.id, true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.reportService
      .resolveReport(report.id, {
        status,
        resolutionNote: this.resolutionNoteByReportId()[report.id]?.trim() || null,
      })
      .pipe(finalize(() => this.setResolving(report.id, false)))
      .subscribe({
        next: (updatedReport) => {
          this.reports.update((reports) => reports.map((item) => (item.id === updatedReport.id ? updatedReport : item)));
          this.resolutionNoteByReportId.update((notes) => ({
            ...notes,
            [report.id]: '',
          }));
          this.successMessage.set(`Report marked as ${status.toLowerCase()}.`);
        },
        error: (error) => this.errorMessage.set(this.errorText(error)),
      });
  }

  updateResolutionNote(reportId: UUID, event: Event): void {
    const input = event.target as HTMLTextAreaElement;
    this.resolutionNoteByReportId.update((notes) => ({
      ...notes,
      [reportId]: input.value,
    }));
  }

  loadReportTarget(report: Report): void {
    if (this.loadingTargetIds()[report.id]) {
      return;
    }

    this.setLoadingTarget(report.id, true);
    this.errorMessage.set(null);

    this.reportService
      .getReportTarget(report.id)
      .pipe(finalize(() => this.setLoadingTarget(report.id, false)))
      .subscribe({
        next: (target) => {
          this.targetByReportId.update((targets) => ({
            ...targets,
            [report.id]: target,
          }));
        },
        error: (error) => this.errorMessage.set(this.errorText(error)),
      });
  }

  moderateTarget(report: Report, target: ReportTarget): void {
    if (this.moderatingTargetIds()[report.id] || !this.canModerateTarget(target)) {
      return;
    }

    if (!window.confirm(`${this.moderationActionLabel(target)}?`)) {
      return;
    }

    const action$ = this.moderationAction(target);
    if (!action$) {
      return;
    }

    this.setModeratingTarget(report.id, true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    action$.pipe(finalize(() => this.setModeratingTarget(report.id, false))).subscribe({
      next: () => {
        this.successMessage.set(this.moderationSuccessMessage(target));
        this.loadReportTarget(report);
      },
      error: (error) => this.errorMessage.set(this.errorText(error)),
    });
  }

  canModerateTarget(target: ReportTarget): boolean {
    switch (target.targetType) {
      case 'POST':
        return target.status !== 'HIDDEN' && target.status !== 'DELETED';
      case 'COMMENT':
        return target.status !== 'HIDDEN' && target.status !== 'DELETED';
      case 'MESSAGE':
        return target.status !== 'DELETED';
      case 'USER':
        return target.userStatus === 'ACTIVE';
      case 'MEDIA':
        return target.status !== 'HIDDEN' && target.status !== 'DELETED';
    }
  }

  moderationActionLabel(target: ReportTarget): string {
    switch (target.targetType) {
      case 'POST':
        return 'Hide post';
      case 'COMMENT':
        return 'Hide comment';
      case 'MESSAGE':
        return 'Delete message';
      case 'USER':
        return 'Suspend user';
      case 'MEDIA':
        return 'Hide media';
    }
  }

  userLabel(user: UserSummary): string {
    return user.displayName ? `${user.displayName} (@${user.username})` : `@${user.username}`;
  }

  formatBytes(sizeBytes: number | null): string {
    if (sizeBytes === null) {
      return 'Unknown size';
    }

    if (sizeBytes < 1024) {
      return `${sizeBytes} B`;
    }

    if (sizeBytes < 1024 * 1024) {
      return `${(sizeBytes / 1024).toFixed(1)} KB`;
    }

    return `${(sizeBytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  isTargetReport(report: Report): boolean {
    return this.targetReportId() === report.id;
  }

  isFinalStatus(status: ReportStatus): boolean {
    return status === 'RESOLVED' || status === 'REJECTED';
  }

  trackReport(_: number, report: Report): UUID {
    return report.id;
  }

  private ensureTargetReportVisible(): void {
    const reportId = this.targetReportId();
    if (!reportId) {
      return;
    }

    if (this.reports().some((report) => report.id === reportId)) {
      this.scrollToTargetReport(reportId);
      return;
    }

    if (this.isLoadingTargetReport()) {
      return;
    }

    this.isLoadingTargetReport.set(true);
    this.reportService
      .getReportById(reportId)
      .pipe(finalize(() => this.isLoadingTargetReport.set(false)))
      .subscribe({
        next: (report) => {
          this.reports.update((reports) => [report, ...reports.filter((item) => item.id !== report.id)]);
          this.scrollToTargetReport(report.id);
        },
        error: (error) => this.errorMessage.set(this.errorText(error)),
      });
  }

  private scrollToTargetReport(reportId: UUID): void {
    window.setTimeout(() => {
      document.getElementById(`report-${reportId}`)?.scrollIntoView({
        behavior: 'smooth',
        block: 'center',
      });
    });
  }

  private setResolving(reportId: UUID, value: boolean): void {
    this.resolvingReportIds.update((ids) => ({
      ...ids,
      [reportId]: value,
    }));
  }

  private setLoadingTarget(reportId: UUID, value: boolean): void {
    this.loadingTargetIds.update((ids) => ({
      ...ids,
      [reportId]: value,
    }));
  }

  private setModeratingTarget(reportId: UUID, value: boolean): void {
    this.moderatingTargetIds.update((ids) => ({
      ...ids,
      [reportId]: value,
    }));
  }

  private moderationAction(target: ReportTarget): Observable<void> | null {
    switch (target.targetType) {
      case 'POST':
        return this.moderationService.hidePost(target.targetId);
      case 'COMMENT':
        return this.moderationService.hideComment(target.targetId);
      case 'MESSAGE':
        return this.moderationService.deleteMessage(target.targetId);
      case 'USER':
        return this.moderationService.suspendUser(target.targetId);
      case 'MEDIA':
        return this.moderationService.hideMedia(target.targetId);
    }
  }

  private moderationSuccessMessage(target: ReportTarget): string {
    switch (target.targetType) {
      case 'POST':
        return 'Post hidden.';
      case 'COMMENT':
        return 'Comment hidden.';
      case 'MESSAGE':
        return 'Message deleted.';
      case 'USER':
        return 'User suspended.';
      case 'MEDIA':
        return 'Media hidden.';
    }
  }

  private selectedReportStatus(): ReportStatus | null {
    const status = this.selectedStatus();
    return status === 'ALL' ? null : status;
  }

  private errorText(error: unknown): string {
    if (this.hasErrorMessage(error)) {
      return error.error.message;
    }

    return 'Unable to load reports. Try again.';
  }

  private hasErrorMessage(error: unknown): error is { error: ErrorResponse } {
    return (
      typeof error === 'object' &&
      error !== null &&
      'error' in error &&
      typeof (error as { error?: unknown }).error === 'object' &&
      (error as { error?: unknown }).error !== null &&
      'message' in ((error as { error?: unknown }).error as Record<string, unknown>) &&
      typeof ((error as { error: { message?: unknown } }).error.message) === 'string'
    );
  }
}
