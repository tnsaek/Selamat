import { UUID } from './common.models';

export interface Profile {
  id: UUID;
  userId: UUID;
  displayName: string | null;
  bio: string | null;
  avatarUrl: string | null;
  coverImageUrl: string | null;
  street: string | null;
  city: string | null;
  state: string | null;
  country: string | null;
  websiteUrl: string | null;
}

export interface UpdateProfileRequest {
  displayName?: string | null;
  bio?: string | null;
  avatarUrl?: string | null;
  coverImageUrl?: string | null;
  street?: string | null;
  city?: string | null;
  state?: string | null;
  country?: string | null;
  websiteUrl?: string | null;
}
