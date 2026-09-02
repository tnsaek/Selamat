import { IsoDateTime, UUID } from './common.models';

export type MediaType = 'IMAGE' | 'VIDEO' | 'AUDIO' | 'DOCUMENT';

export interface Media {
  id: UUID;
  url: string;
  mediaType: MediaType;
  mimeType: string;
  sizeBytes: number;
  altText: string | null;
  createdAt: IsoDateTime;
}
