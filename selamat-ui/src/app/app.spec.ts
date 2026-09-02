import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { App } from './app';
import { SessionTimeoutService } from './core/services';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter([]),
        {
          provide: SessionTimeoutService,
          useValue: {
            start: vi.fn(),
          },
        },
      ],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should start session timeout monitoring', () => {
    const sessionTimeoutService = TestBed.inject(SessionTimeoutService);

    TestBed.createComponent(App);

    expect(sessionTimeoutService.start).toHaveBeenCalledOnce();
  });
});
