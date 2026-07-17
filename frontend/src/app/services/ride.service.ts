import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { AuthService } from './auth.service';
import { Client } from '@stomp/stompjs';

@Injectable({
  providedIn: 'root'
})
export class RideService {
  private apiUrl = 'http://localhost:8080/api';
  private stompClient: Client | null = null;
  private messageSubject = new Subject<any>();
  public messages$ = this.messageSubject.asObservable();

  constructor(private http: HttpClient, private authService: AuthService) {}

  // 1. Availability Endpoints
  public getAvailabilityMe(): Observable<any> {
    return this.http.get(`${this.apiUrl}/availabilities/me`);
  }

  public setAvailability(payload: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/availabilities`, payload);
  }

  public deactivateAvailability(): Observable<any> {
    return this.http.put(`${this.apiUrl}/availabilities/deactivate`, {});
  }

  public getCompatibleMatches(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/availabilities/compatible`);
  }

  // 2. Request Endpoints
  public getIncomingRequests(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/requests/incoming`);
  }

  public getOutgoingRequests(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/requests/outgoing`);
  }

  public sendRequest(receiverId: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/requests/send/${receiverId}`, {});
  }

  public acceptRequest(requestId: number): Observable<any> {
    return this.http.put(`${this.apiUrl}/requests/${requestId}/accept`, {});
  }

  public rejectRequest(requestId: number): Observable<any> {
    return this.http.put(`${this.apiUrl}/requests/${requestId}/reject`, {});
  }

  public cancelRequest(requestId: number): Observable<any> {
    return this.http.put(`${this.apiUrl}/requests/${requestId}/cancel`, {});
  }

  // 3. Match Endpoints
  public getActiveMatch(): Observable<any> {
    return this.http.get(`${this.apiUrl}/matches/active`);
  }

  public completeMatch(matchId: number): Observable<any> {
    return this.http.put(`${this.apiUrl}/matches/${matchId}/complete`, {});
  }

  public cancelMatch(matchId: number): Observable<any> {
    return this.http.put(`${this.apiUrl}/matches/${matchId}/cancel`, {});
  }

  public getMatchHistory(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/matches/history`);
  }

  // 4. Chat Endpoints
  public getChatHistory(matchId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/chats/${matchId}/messages`);
  }

  // 5. WebSocket Chat Coordination
  public connectToChat(matchId: number): void {
    if (this.stompClient && this.stompClient.connected) {
      return;
    }

    const token = this.authService.getAccessToken();
    if (!token) {
      console.error('Cannot connect to WebSocket: No access token');
      return;
    }

    // Configure stompjs client
    this.stompClient = new Client({
      brokerURL: 'ws://localhost:8080/ws',
      connectHeaders: {
        Authorization: `Bearer ${token}`
      },
      debug: (str) => console.log('STOMP: ' + str),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000
    });

    this.stompClient.onConnect = (frame) => {
      console.log('Connected to WebSocket successfully');
      
      // Subscribe to match specific chat topic
      this.stompClient?.subscribe(`/topic/match/${matchId}/chat`, (message) => {
        if (message.body) {
          const chatMsg = JSON.parse(message.body);
          this.messageSubject.next(chatMsg);
        }
      });
    };

    this.stompClient.onStompError = (frame) => {
      console.error('Broker reported error: ' + frame.headers['message']);
      console.error('Additional details: ' + frame.body);
    };

    this.stompClient.activate();
  }

  public sendMessage(matchId: number, content: string): void {
    if (!this.stompClient || !this.stompClient.connected) {
      console.error('STOMP client not connected, unable to send message');
      return;
    }

    const payload = { content };
    this.stompClient.publish({
      destination: `/app/chat/${matchId}`,
      body: JSON.stringify(payload)
    });
  }

  public disconnectFromChat(): void {
    if (this.stompClient) {
      this.stompClient.deactivate();
      this.stompClient = null;
      console.log('Disconnected STOMP client');
    }
  }
}
