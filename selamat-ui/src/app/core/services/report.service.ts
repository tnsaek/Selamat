import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import {
  CreateReportRequest,
  AdminReportSummary,
  PageResponse,
  Report,
  ReportStatus,
  ReportTarget,
  ResolveReportRequest,
  UUID,
} from '../../models';
import { ApiService } from './api.service';

export interface ReportQuery {
  status?: ReportStatus | null;
  cursor?: string | null;
  limit?: number | null;
}

@Injectable({
  providedIn: 'root',
})
export class ReportService {
  private readonly api = inject(ApiService);

  createReport(request: CreateReportRequest): Observable<Report> {
    return this.api.post<Report, CreateReportRequest>('/api/reports', request);
  }

  listReports(query: ReportQuery = {}): Observable<PageResponse<Report>> {
    return this.api.get<PageResponse<Report>>('/api/admin/reports', {
      status: query.status,
      cursor: query.cursor,
      limit: query.limit,
    });
  }

  getAdminSummary(): Observable<AdminReportSummary> {
    return this.api.get<AdminReportSummary>('/api/admin/reports/summary');
  }

  getReportById(reportId: UUID): Observable<Report> {
    return this.api.get<Report>(`/api/admin/reports/${reportId}`);
  }

  getReportTarget(reportId: UUID): Observable<ReportTarget> {
    return this.api.get<ReportTarget>(`/api/admin/reports/${reportId}/target`);
  }

  resolveReport(reportId: UUID, request: ResolveReportRequest): Observable<Report> {
    return this.api.patch<Report, ResolveReportRequest>(`/api/admin/reports/${reportId}/resolve`, request);
  }
}
