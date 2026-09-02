import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Media } from '../../models';
import { ApiService } from './api.service';

export interface UploadMediaRequest {
  file: File;
  altText?: string | null;
}

@Injectable({
  providedIn: 'root',
})
export class MediaService {
  private readonly api = inject(ApiService);
  private readonly http = inject(HttpClient);

  uploadMedia(request: UploadMediaRequest): Observable<Media> {
    const formData = new FormData();
    formData.append('file', request.file);

    if (request.altText?.trim()) {
      formData.append('altText', request.altText.trim());
    }

    return this.http.post<Media>(this.api.absoluteUrl('/api/media'), formData);
  }
}
