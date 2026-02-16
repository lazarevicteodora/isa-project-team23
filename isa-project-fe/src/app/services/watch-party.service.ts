import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { map } from 'rxjs/operators';

// SockJS i Stomp se učitavaju preko <script> tagova u index.html
declare var SockJS: any;
declare var Stomp: any;

export interface WatchPartyRoom {
  roomCode: string;
  videoId: number;
  videoTitle: string;
  creatorUsername: string;
  members: string[];
  memberCount: number;
  active: boolean;
}

export interface WatchPartyEvent {
  type: 'VIDEO_STARTED' | 'USER_JOINED' | 'USER_LEFT';
  videoId?: number;
  videoTitle?: string;
  startedBy?: string;
  username?: string;
  memberCount?: number;
  members?: string[];
}

@Injectable({
  providedIn: 'root'
})
export class WatchPartyService {

  private apiUrl = 'http://localhost:8080/api/watch-party';
  private wsUrl = 'http://localhost:8080/ws';

  private stompClient: any = null;
  private eventSubject = new Subject<WatchPartyEvent>();

  public events$ = this.eventSubject.asObservable();

  constructor(private http: HttpClient) {}

  // ==================== REST CALLS ====================

  createRoom(videoId: number, videoTitle: string): Observable<WatchPartyRoom> {
    return this.http.post<WatchPartyRoom>(`${this.apiUrl}/create`, {
      videoId,
      videoTitle
    });
  }

  joinRoom(roomCode: string): Observable<WatchPartyRoom> {
    return this.http.post<WatchPartyRoom>(`${this.apiUrl}/join/${roomCode}`, {});
  }

  leaveRoom(roomCode: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/leave/${roomCode}`, {});
  }

  getRoom(roomCode: string): Observable<WatchPartyRoom> {
    return this.http.get<WatchPartyRoom>(`${this.apiUrl}/room/${roomCode}`);
  }

  getAllRooms(): Observable<WatchPartyRoom[]> {
    return this.http.get<WatchPartyRoom[]>(`${this.apiUrl}/rooms`);
  }

  /**
   * FIX: Uzima username od backenda — jer token.sub je email, ne username.
   * Backend proverava JWT i vraća username ulogovanog korisnika.
   */
  getCurrentUsername(): Observable<string> {
    return this.http.get<{ username: string }>(`http://localhost:8080/api/watch-party/me`)
      .pipe(map(res => res.username));
  }

  // ==================== WEBSOCKET ====================

  connectToRoom(roomCode: string): void {
    const socket = new SockJS(this.wsUrl);
    this.stompClient = Stomp.over(socket);

    this.stompClient.debug = null;

    const token = localStorage.getItem('token');
    const headers: any = {};

    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    this.stompClient.connect(headers,
      () => {
        console.log('✅ WebSocket connected to room:', roomCode);

        this.stompClient.subscribe(`/topic/room/${roomCode}`, (message: any) => {
          const event: WatchPartyEvent = JSON.parse(message.body);
          console.log('📺 Received event:', event);
          this.eventSubject.next(event);
        });
      },
      (error: any) => {
        console.error('❌ WebSocket connection error:', error);
      }
    );
  }

  sendPlayEvent(roomCode: string): void {
    if (!this.stompClient || !this.stompClient.connected) {
      console.error('❌ WebSocket not connected!');
      return;
    }

    const payload = {
      roomCode: roomCode,
      action: 'play'
    };

    this.stompClient.send(
      '/app/watch-party/play',
      {},
      JSON.stringify(payload)
    );

    console.log('▶️ Play event sent for room:', roomCode);
  }

  disconnect(): void {
    if (this.stompClient && this.stompClient.connected) {
      this.stompClient.disconnect(() => {
        console.log('🔌 WebSocket disconnected');
      });
    }
  }
}