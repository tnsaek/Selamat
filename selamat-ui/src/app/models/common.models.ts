export type UUID = string;
export type IsoDateTime = string;

export interface PageResponse<T> {
  items: T[];
  nextCursor: string | null;
}

export interface ErrorResponse {
  code: string;
  message: string;
  details: string[];
}
