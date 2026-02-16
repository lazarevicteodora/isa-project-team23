import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { ChatMessage, ActiveUsersResponse } from '../models/chat-message.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class StreamChatService {
  private ws: WebSocket | null = null;
  private messagesSubject = new BehaviorSubject<ChatMessage[]>([]);
  private activeUsersSubject = new BehaviorSubject<number>(0);
  private connectionStatusSubject = new BehaviorSubject<'connected' | 'disconnected' | 'error'>('disconnected');
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectDelay = 3000; // 3 sekunde
  private messageBuffer: ChatMessage[] = [];
  private readonly MAX_MESSAGES = 100;

  private currentVideoId: number | null = null;
  private currentUsername: string | null = null;

  public messages$ = this.messagesSubject.asObservable();
  public activeUsers$ = this.activeUsersSubject.asObservable();
  public connectionStatus$ = this.connectionStatusSubject.asObservable();

  constructor(private http: HttpClient) {}

  /**
   * Konektuj se na WebSocket za streaming chat
   */
  connect(videoId: number, username: string): void {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      console.log('WebSocket je već konektovan');
      return;
    }

    this.currentVideoId = videoId;
    this.currentUsername = username;

    const wsUrl = `${environment.wsUrl}/ws/stream-chat/${videoId}`;
    console.log(`🔌 Pokušaj konekcije na: ${wsUrl}`);
    console.log(`👤 Korisnik: ${username}`);
    
    try {
      this.ws = new WebSocket(wsUrl);
      
      this.ws.onopen = () => {
        console.log('✅ WebSocket konekcija uspostavljena za video:', videoId);
        this.connectionStatusSubject.next('connected');
        this.reconnectAttempts = 0;
        this.loadActiveUsers(videoId);

        // NE ŠALJI "join" poruku - samo se konektuj
        // Backend automatski dodaje korisnika u addUserSession()
      };

      this.ws.onmessage = (event: MessageEvent) => {
        console.log('📥 RAW MESSAGE PRIMLJENA:', event.data);
        try {
          const data = JSON.parse(event.data);
          
          if (data.type === 'USER_COUNT') {
            console.log('👥 USER COUNT UPDATE:', data.count);
            this.activeUsersSubject.next(data.count);
          } else {
            const message = data as ChatMessage;
            console.log('💬 PARSIRANA PORUKA:', message);
            this.addMessageToBuffer(message);
          }
        } catch (error) {
          console.error('❌ GREŠKA PRI PARSIRANJU:', error);
        }
      };

      this.ws.onerror = (error: Event) => {
        console.error('❌ WebSocket greška:', error);
        this.connectionStatusSubject.next('error');
      };

      this.ws.onclose = (event: CloseEvent) => {
        console.log('🔌 WebSocket konekcija zatvorena');
        console.log('Close code:', event.code, 'Reason:', event.reason);
        this.connectionStatusSubject.next('disconnected');
        
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
          this.reconnectAttempts++;
          const delay = this.reconnectDelay * Math.pow(2, this.reconnectAttempts - 1);
          console.log(`⏳ Pokušaj ponovno konekcije za ${delay}ms (pokušaj ${this.reconnectAttempts}/${this.maxReconnectAttempts})`);
          
          setTimeout(() => {
            console.log(`🔄 Ponovni pokušaj konekcije #${this.reconnectAttempts}...`);
            this.connect(videoId, username);
          }, delay);
        } else {
          console.error('❌ Maksimalan broj pokušaja ponovno konekcije dostignut');
        }
      };

    } catch (error) {
      console.error('Greška pri kreiranju WebSocket konekcije:', error);
      this.connectionStatusSubject.next('error');
    }
  }

  /**
   * Pošalji poruku preko WebSocket-a
   * PRIMA STRING, KREIRA ChatMessage OBJEKAT
   */
  sendMessage(messageText: string): void {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      console.warn('⚠️ WebSocket nije spreman. Status:', this.ws?.readyState);
      return;
    }

    if (!this.currentVideoId || !this.currentUsername) {
      console.error('❌ videoId ili username nisu postavljeni!');
      return;
    }

    try {
      // Kreiraj ChatMessage objekat
      const chatMessage = {
        videoId: this.currentVideoId,
        username: this.currentUsername,
        message: messageText
        // NE ŠALJI timestamp - Backend će ga kreirati!
      };

      console.log('📤 ŠALJEM PORUKU:', chatMessage);
      this.ws.send(JSON.stringify(chatMessage));
      
    } catch (error) {
      console.error('❌ Greška pri slanju poruke:', error);
    }
  }

  /**
   * Dodaj poruku u buffer i updateuj subject
   */
  private addMessageToBuffer(message: ChatMessage): void {
    console.log('➕ DODAVANJE PORUKE U BUFFER:', message);
    console.log('➕ USERNAME:', message.username);
    console.log('➕ MESSAGE:', message.message);
    console.log('➕ TIMESTAMP:', message.timestamp);
    
    this.messageBuffer.push(message);
    
    if (this.messageBuffer.length > this.MAX_MESSAGES) {
      this.messageBuffer.shift();
    }
    
    this.messagesSubject.next([...this.messageBuffer]);
    console.log('✅ BUFFER NAKON DODAVANJA:', this.messageBuffer);
  }

  /**
   * Učitaj broj aktivnih korisnika
   */
  loadActiveUsers(videoId: number): void {
    const apiUrl = `${environment.apiUrl}/stream-chat/active-users/${videoId}`;
    console.log(`📊 Učitaj aktivne korisnike sa: ${apiUrl}`);
    
    this.http.get<ActiveUsersResponse>(apiUrl).subscribe({
      next: (response) => {
        this.activeUsersSubject.next(response.activeUsers);
        console.log(`👥 Aktivnih korisnika: ${response.activeUsers}`);
      },
      error: (error) => {
        console.error('Greška pri učitavanju broja aktivnih korisnika:', error);
      }
    });
  }

  /**
   * Prekini WebSocket konekciju
   */
  disconnect(): void {
    if (this.ws) {
      this.ws.close();
      this.ws = null;
      this.messageBuffer = [];
      this.messagesSubject.next([]);
      this.connectionStatusSubject.next('disconnected');
      this.currentVideoId = null;
      this.currentUsername = null;
      console.log('🔌 WebSocket konekcija prekinuta');
    }
  }

  /**
   * Preuzmi sve poruke iz buffer-a
   */
  getMessages(): ChatMessage[] {
    return this.messageBuffer;
  }

  /**
   * Očisti sve poruke
   */
  clearMessages(): void {
    this.messageBuffer = [];
    this.messagesSubject.next([]);
  }
}