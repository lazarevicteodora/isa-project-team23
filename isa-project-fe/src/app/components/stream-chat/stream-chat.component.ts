import { Component, OnInit, OnDestroy, Input, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { StreamChatService } from '../../services/stream-chat.service';
import { ChatMessage } from '../../models/chat-message.model';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-stream-chat',
  templateUrl: './stream-chat.component.html',
  styleUrls: ['./stream-chat.component.css']
})
export class StreamChatComponent implements OnInit, OnDestroy, AfterViewChecked {
  @Input() videoId!: number;
  @Input() username!: string;

  @ViewChild('chatMessages') chatMessagesContainer!: ElementRef<HTMLDivElement>;

  messages: ChatMessage[] = [];
  newMessage: string = '';
  activeUsers: number = 0;
  connectionStatus: 'connected' | 'disconnected' | 'error' = 'disconnected';
  isSubmittingMessage: boolean = false;
  chatOpen: boolean = true;
  errorMessage: string = '';
  retryAttempts: number = 0;
  maxRetries: number = 3;
  wsUrl: string = '';

  private destroy$ = new Subject<void>();
  private shouldScroll: boolean = false;

  constructor(private streamChatService: StreamChatService) {}

  ngOnInit(): void {
    console.log(`🚀 Stream Chat inicijalizacija za video: ${this.videoId}, korisnik: ${this.username}`);
    
    // Validacija
    if (!this.videoId || !this.username) {
      console.error('❌ Stream Chat init failed: videoId ili username nisu postavljeni');
      this.errorMessage = 'Greška pri inicijalizaciji četa';
      this.connectionStatus = 'error';
      return;
    }
    
    // Konektuj se na WebSocket
    this.streamChatService.connect(this.videoId, this.username);

    // Pretplati se na poruke
    this.streamChatService.messages$
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (messages) => {
          this.messages = messages;
          this.shouldScroll = true;
          this.errorMessage = '';
        }
      });

    // Pretplati se na status konekcije
    this.streamChatService.connectionStatus$
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (status) => {
          this.connectionStatus = status;
          console.log(`📡 Stanje konekcije: ${status}`);
          
          if (status === 'connected') {
            this.errorMessage = '';
            this.retryAttempts = 0;
          } else if (status === 'error') {
            this.errorMessage = 'Nije moguće uspostaviti konekciju sa server-om za chat.';
            if (this.retryAttempts < this.maxRetries) {
              this.retryAttempts++;
            }
          } else if (status === 'disconnected') {
            this.errorMessage = 'Chat je odspojen. Pokušavam ponovno konekciju...';
          }
        }
      });

    // Pretplati se na broj aktivnih korisnika
    this.streamChatService.activeUsers$
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (users) => {
          this.activeUsers = users;
        }
      });
  }

  ngAfterViewChecked(): void {
    // Automatski skrol na dno kada nove poruke stignu
    if (this.shouldScroll && this.chatMessagesContainer) {
      this.chatMessagesContainer.nativeElement.scrollTop =
        this.chatMessagesContainer.nativeElement.scrollHeight;
      this.shouldScroll = false;
    }
  }

  /**
   * Pošalji novu poruku
   */
  sendMessage(): void {
    console.log(`📤 sendMessage() pozvan - Status: ${this.connectionStatus}, Tekst: ${this.newMessage.trim()}`);
    
    if (!this.newMessage.trim()) {
      console.warn('⚠️ Poruka je prazna');
      return;
    }
    
    if (this.connectionStatus !== 'connected') {
      console.warn(`⚠️ WebSocket nije konekcije. Status: ${this.connectionStatus}`);
      alert('❌ Chat nije konekcije. Pokušajte da osvežite stranicu.');
      return;
    }

    this.isSubmittingMessage = true;
    console.log('📨 Slanjem poruka u servis...');

    // Pošalji samo tekst poruke - servis će kreirati ChatMessage objekat
    this.streamChatService.sendMessage(this.newMessage.trim());
    
    this.newMessage = '';
    this.isSubmittingMessage = false;
  }

  /**
   * Pokušaj ponovno konekciju
   */
  retryConnection(): void {
    console.log('🔄 Pokušaj ponovno konekcije...');
    this.errorMessage = '';
    this.streamChatService.disconnect();
    setTimeout(() => {
      this.streamChatService.connect(this.videoId, this.username);
    }, 1000);
  }

  /**
   * Otvori/Zatvori chat
   */
  toggleChat(): void {
    this.chatOpen = !this.chatOpen;
  }

  /**
   * Formatiraj vremensku oznaku za prikaz
   */
  formatTime(timestamp: string | undefined): string {
    console.log('🕐 Parsing timestamp:', timestamp);
    
    if (!timestamp) {
      console.warn('⚠️ Empty timestamp, returning placeholder');
      return '--:--';
    }
    
    const date = new Date(timestamp);
    if (isNaN(date.getTime())) {
      console.error('❌ Invalid timestamp format:', timestamp);
      return '--:--';
    }
    
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${hours}:${minutes}`;
  }

  ngOnDestroy(): void {
    this.streamChatService.disconnect();
    this.destroy$.next();
    this.destroy$.complete();
  }
}