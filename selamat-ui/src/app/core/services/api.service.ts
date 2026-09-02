import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config';

type QueryValue = string | number | boolean | null | undefined;
type QueryParams = Record<string, QueryValue>;

@Injectable({
  providedIn: 'root',
})
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  get<T>(path: string, params?: QueryParams): Observable<T> {
    return this.http.get<T>(this.url(path), {
      params: this.params(params),
    });
  }

  post<TResponse, TBody = unknown>(path: string, body: TBody, params?: QueryParams): Observable<TResponse> {
    return this.http.post<TResponse>(this.url(path), body, {
      params: this.params(params),
    });
  }

  put<TResponse, TBody = unknown>(path: string, body: TBody, params?: QueryParams): Observable<TResponse> {
    return this.http.put<TResponse>(this.url(path), body, {
      params: this.params(params),
    });
  }

  patch<TResponse, TBody = unknown>(path: string, body: TBody, params?: QueryParams): Observable<TResponse> {
    return this.http.patch<TResponse>(this.url(path), body, {
      params: this.params(params),
    });
  }

  delete<T>(path: string, params?: QueryParams): Observable<T> {
    return this.http.delete<T>(this.url(path), {
      params: this.params(params),
    });
  }

  absoluteUrl(path: string): string {
    return this.url(path);
  }

  private url(path: string): string {
    const normalizedBaseUrl = this.apiBaseUrl.replace(/\/$/, '');
    const normalizedPath = path.startsWith('/') ? path : `/${path}`;
    return `${normalizedBaseUrl}${normalizedPath}`;
  }

  private params(params?: QueryParams): HttpParams {
    let httpParams = new HttpParams();

    for (const [key, value] of Object.entries(params ?? {})) {
      if (value !== null && value !== undefined && value !== '') {
        httpParams = httpParams.set(key, String(value));
      }
    }

    return httpParams;
  }
}
