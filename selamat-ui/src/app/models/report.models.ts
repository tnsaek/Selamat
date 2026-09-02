import { IsoDateTime, UUID } from './common.models';
import { MediaType } from './media.models';
import { UserStatus, UserSummary } from './user.models';

export type ReportStatus = 'OPEN' | 'UNDER_REVIEW' | 'RESOLVED' | 'REJECTED';
export type ReportTargetType = 'USER' | 'POST' | 'COMMENT' | 'MESSAGE' | 'MEDIA';

export interface Report {
  id: UUID;
  reporter: UserSummary;
  resolver: UserSummary | null;
  targetType: ReportTargetType;
  targetId: UUID;
  reason: string;
  details: string | null;
  resolutionNote: string | null;
  status: ReportStatus;
  createdAt: IsoDateTime;
  resolvedAt: IsoDateTime | null;
}

export interface CreateReportRequest {
  targetType: ReportTargetType;
  targetId: UUID;
  reason: string;
  details?: string | null;
}

export interface ResolveReportRequest {
  status: Extract<ReportStatus, 'RESOLVED' | 'REJECTED'>;
  resolutionNote?: string | null;
}

export interface AdminReportSummary {
  openReports: number;
  underReviewReports: number;
  resolvedReports: number;
  rejectedReports: number;
  userReports: number;
  contentReports: number;
  mediaReports: number;
}

export interface ReportTargetMediaPreview {
  id: UUID;
  url: string;
  mediaType: MediaType;
  mimeType: string;
  sizeBytes: number;
  altText: string | null;
}

export interface ReportTarget {
  reportId: UUID;
  targetType: ReportTargetType;
  targetId: UUID;
  user: UserSummary | null;
  email: string | null;
  userStatus: UserStatus | null;
  bio: string | null;
  author: UserSummary | null;
  sender: UserSummary | null;
  recipient: UserSummary | null;
  uploader: UserSummary | null;
  postId: UUID | null;
  content: string | null;
  status: string | null;
  visibility: string | null;
  commentCount: number | null;
  reactionCount: number | null;
  url: string | null;
  mediaType: MediaType | null;
  mimeType: string | null;
  sizeBytes: number | null;
  altText: string | null;
  createdAt: IsoDateTime | null;
  media: ReportTargetMediaPreview[];
}
