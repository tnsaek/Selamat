import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ReportService } from '../../core/services';
import { AdminReportSummary } from '../../models';

@Component({
  selector: 'app-admin-dashboard-page',
  imports: [RouterLink],
  templateUrl: './admin-dashboard-page.html',
  styleUrl: './admin-dashboard-page.scss',
})
export class AdminDashboardPage implements OnInit {
  private readonly reportService = inject(ReportService);

  readonly summary = signal<AdminReportSummary | null>(null);
  readonly isLoadingSummary = signal(true);
  readonly summaryError = signal<string | null>(null);

  readonly metrics = computed(() => {
    const summary = this.summary();
    return [
      {
        label: 'Open reports',
        value: summary?.openReports ?? 0,
      },
      {
        label: 'Under review',
        value: summary?.underReviewReports ?? 0,
      },
      {
        label: 'Resolved reports',
        value: summary?.resolvedReports ?? 0,
      },
      {
        label: 'Media reports',
        value: summary?.mediaReports ?? 0,
      },
    ];
  });

  readonly sections = [
    {
      title: 'Reports',
      description: 'Review user reports, inspect reported targets, and resolve or reject moderation cases.',
      route: '/admin/reports',
      action: 'Open reports',
      status: 'Available',
    },
    {
      title: 'Users',
      description: 'Suspend unsafe accounts, review user status, and manage account-level moderation.',
      route: null,
      action: 'Coming soon',
      status: 'Planned',
    },
    {
      title: 'Content',
      description: 'Review posts, comments, messages, and media that require moderator action.',
      route: null,
      action: 'Coming soon',
      status: 'Planned',
    },
  ];

  ngOnInit(): void {
    this.loadSummary();
  }

  loadSummary(): void {
    this.isLoadingSummary.set(true);
    this.summaryError.set(null);

    this.reportService.getAdminSummary().subscribe({
      next: (summary) => {
        this.summary.set(summary);
        this.isLoadingSummary.set(false);
      },
      error: () => {
        this.summaryError.set('Unable to load dashboard metrics.');
        this.isLoadingSummary.set(false);
      },
    });
  }

  trackMetric(_: number, metric: { label: string }): string {
    return metric.label;
  }

  trackSection(_: number, section: { title: string }): string {
    return section.title;
  }
}
