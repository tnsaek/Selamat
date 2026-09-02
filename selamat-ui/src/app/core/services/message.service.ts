import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Message, PageResponse, SendMessageRequest, UUID } from '../../models';
import { ApiService } from './api.service';

export interface MessageQuery {
  cursor?: string | null;
  limit?: number | null;
}

@Injectable({
  providedIn: 'root',
})
export class MessageService {
  private readonly api = inject(ApiService);

  listMessages(query: MessageQuery = {}): Observable<PageResponse<Message>> {
    return this.api.get<PageResponse<Message>>('/api/messages', {
      cursor: query.cursor,
      limit: query.limit,
    });
  }

  getMessageById(messageId: UUID): Observable<Message> {
    return this.api.get<Message>(`/api/messages/${messageId}`);
  }

  sendMessage(request: SendMessageRequest): Observable<Message> {
    return this.api.post<Message, SendMessageRequest>('/api/messages', request);
  }

  markConversationAsRead(participantId: UUID): Observable<void> {
    return this.api.patch<void, Record<string, never>>(`/api/messages/conversations/${participantId}/read`, {});
  }
}
