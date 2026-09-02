import { HttpClient, HttpParams } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { vi } from 'vitest';

import { API_BASE_URL } from '../config';
import { ApiService } from './api.service';

describe('ApiService', () => {
  const httpClient = {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  };

  let apiService: ApiService;

  beforeEach(() => {
    vi.clearAllMocks();
    for (const method of Object.values(httpClient)) {
      method.mockReturnValue(of({ ok: true }));
    }

    TestBed.configureTestingModule({
      providers: [
        ApiService,
        { provide: HttpClient, useValue: httpClient },
        { provide: API_BASE_URL, useValue: 'https://api.example.com/' },
      ],
    });
    apiService = TestBed.inject(ApiService);
  });

  it('builds absolute URLs with or without a leading slash', () => {
    expect(apiService.absoluteUrl('/api/users')).toBe('https://api.example.com/api/users');
    expect(apiService.absoluteUrl('api/users')).toBe('https://api.example.com/api/users');
  });

  it('sends GET requests with normalized query params', () => {
    apiService.get('/api/users', {
      query: 'tinsae',
      page: 2,
      active: true,
      empty: '',
      missing: null,
      skipped: undefined,
    }).subscribe();

    const options = httpClient.get.mock.calls[0][1] as { params: HttpParams };
    expect(httpClient.get).toHaveBeenCalledWith('https://api.example.com/api/users', {
      params: expect.any(HttpParams),
    });
    expect(options.params.get('query')).toBe('tinsae');
    expect(options.params.get('page')).toBe('2');
    expect(options.params.get('active')).toBe('true');
    expect(options.params.has('empty')).toBe(false);
    expect(options.params.has('missing')).toBe(false);
    expect(options.params.has('skipped')).toBe(false);
  });

  it('forwards POST, PUT, PATCH, and DELETE requests', () => {
    const body = { name: 'Selam' };

    apiService.post('/api/items', body).subscribe();
    apiService.put('/api/items/1', body).subscribe();
    apiService.patch('/api/items/1', body).subscribe();
    apiService.delete('/api/items/1').subscribe();

    expect(httpClient.post).toHaveBeenCalledWith('https://api.example.com/api/items', body, {
      params: expect.any(HttpParams),
    });
    expect(httpClient.put).toHaveBeenCalledWith('https://api.example.com/api/items/1', body, {
      params: expect.any(HttpParams),
    });
    expect(httpClient.patch).toHaveBeenCalledWith('https://api.example.com/api/items/1', body, {
      params: expect.any(HttpParams),
    });
    expect(httpClient.delete).toHaveBeenCalledWith('https://api.example.com/api/items/1', {
      params: expect.any(HttpParams),
    });
  });
});
