import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { ReportService } from '../../core/services';
import { AdminDashboardPage } from './admin-dashboard-page';

describe('AdminDashboardPage', () => {
  let fixture: ComponentFixture<AdminDashboardPage>;
  let reportService: {
    getAdminSummary: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    reportService = {
      getAdminSummary: vi.fn(() =>
        of({
          openReports: 4,
          underReviewReports: 2,
          resolvedReports: 8,
          rejectedReports: 1,
          userReports: 3,
          contentReports: 7,
          mediaReports: 5,
        }),
      ),
    };

    await TestBed.configureTestingModule({
      imports: [AdminDashboardPage],
      providers: [provideRouter([]), { provide: ReportService, useValue: reportService }],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminDashboardPage);
    fixture.detectChanges();
  });

  it('renders admin dashboard sections', () => {
    const text = fixture.nativeElement.textContent;

    expect(text).toContain('Admin Dashboard');
    expect(text).toContain('Reports');
    expect(text).toContain('Users');
    expect(text).toContain('Content');
    expect(text).toContain('Open reports');
    expect(text).toContain('Under review');
    expect(text).toContain('Resolved reports');
    expect(text).toContain('Media reports');
    expect(text).toContain('4');
    expect(text).toContain('2');
    expect(text).toContain('8');
    expect(text).toContain('5');
  });

  it('loads real dashboard summary metrics', () => {
    expect(reportService.getAdminSummary).toHaveBeenCalledOnce();
    expect(fixture.componentInstance.summary()).toEqual({
      openReports: 4,
      underReviewReports: 2,
      resolvedReports: 8,
      rejectedReports: 1,
      userReports: 3,
      contentReports: 7,
      mediaReports: 5,
    });
    expect(fixture.nativeElement.textContent).toContain('User reports');
    expect(fixture.nativeElement.textContent).toContain('Content reports');
    expect(fixture.nativeElement.textContent).toContain('Rejected reports');
  });

  it('shows a summary load error', async () => {
    TestBed.resetTestingModule();
    reportService = {
      getAdminSummary: vi.fn(() => throwError(() => new Error('boom'))),
    };

    await TestBed.configureTestingModule({
      imports: [AdminDashboardPage],
      providers: [provideRouter([]), { provide: ReportService, useValue: reportService }],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminDashboardPage);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Unable to load dashboard metrics.');
  });

  it('links to the reports admin page', () => {
    const link = fixture.nativeElement.querySelector('a') as HTMLAnchorElement | null;

    expect(link?.textContent?.trim()).toBe('Open reports');
    expect(link?.getAttribute('href')).toBe('/admin/reports');
  });
});
