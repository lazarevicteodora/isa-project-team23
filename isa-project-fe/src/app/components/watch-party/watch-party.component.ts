import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { WatchPartyService, WatchPartyRoom, WatchPartyEvent } from '../../services/watch-party.service';

@Component({
  selector: 'app-watch-party',
  templateUrl: './watch-party.component.html',
  styleUrls: ['./watch-party.component.css']
})
export class WatchPartyComponent implements OnInit, OnDestroy {

  room: WatchPartyRoom | null = null;
  loading: boolean = true;
  errorMessage: string = '';
  videoStarted: boolean = false;
  currentUsername: string = '';
  isCreator: boolean = false;
  members: string[] = [];
  copySuccess: boolean = false;

  private eventSubscription?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private watchPartyService: WatchPartyService
  ) {}

  ngOnInit(): void {
    const roomCode = this.route.snapshot.params['roomCode'];
    if (roomCode) {
      this.loadRoom(roomCode);
    } else {
      this.errorMessage = 'Neispravan link sobe!';
      this.loading = false;
    }
  }

  ngOnDestroy(): void {
    if (this.room) {
      this.watchPartyService.leaveRoom(this.room.roomCode).subscribe();
    }
    this.watchPartyService.disconnect();
    this.eventSubscription?.unsubscribe();
  }

  loadRoom(roomCode: string): void {
    this.watchPartyService.joinRoom(roomCode).subscribe({
      next: (room) => {
        this.room = room;
        this.members = Array.from(room.members || []);

        this.watchPartyService.getCurrentUsername().subscribe({
          next: (username) => {
            this.currentUsername = username;
            this.isCreator = room.creatorUsername === this.currentUsername;
          },
          error: () => {
            this.currentUsername = this.getUsernameFromToken();
            this.isCreator = room.creatorUsername === this.currentUsername;
          }
        });

        this.loading = false;

        this.eventSubscription = this.watchPartyService.events$.subscribe(
          (event: WatchPartyEvent) => this.handleEvent(event)
        );

        this.watchPartyService.connectToRoom(roomCode);
      },
      error: () => {
        this.errorMessage = 'Soba nije pronađena ili nije aktivna!';
        this.loading = false;
      }
    });
  }

  handleEvent(event: WatchPartyEvent): void {
    switch (event.type) {
      case 'VIDEO_STARTED':
        if (!this.isCreator && event.videoId) {
          setTimeout(() => {
            this.router.navigate(['/video', event.videoId]);
          }, 1000);
        }
        break;
      case 'USER_JOINED':
        if (event.members) {
          this.members = Array.from(event.members as any);
        } else if (event.username && !this.members.includes(event.username)) {
          this.members = [...this.members, event.username];
        }
        break;
      case 'USER_LEFT':
        if (event.username) {
          this.members = this.members.filter(m => m !== event.username);
        }
        break;
    }
  }

  startVideo(): void {
    if (!this.room || !this.isCreator) return;
    this.watchPartyService.sendPlayEvent(this.room.roomCode);
    setTimeout(() => {
      this.router.navigate(['/video', this.room!.videoId]);
    }, 500);
  }

  // Kopira samo KOD (npr. 692E9E)
  copyRoomCode(): void {
    if (!this.room) return;
    navigator.clipboard.writeText(this.room.roomCode).then(() => {
      this.copySuccess = true;
      setTimeout(() => this.copySuccess = false, 2000);
    }).catch(() => {
      prompt('Kopirajte kod sobe:', this.room!.roomCode);
    });
  }

  // Kopira cijeli link za dijeljenje
  copyRoomLink(): void {
    if (!this.room) return;
    const link = `${window.location.origin}/watch-party/${this.room.roomCode}`;
    navigator.clipboard.writeText(link).then(() => {
      this.copySuccess = true;
      setTimeout(() => this.copySuccess = false, 2000);
    }).catch(() => {
      prompt('Kopirajte link:', link);
    });
  }

  leaveRoom(): void {
    if (!this.room) return;
    this.watchPartyService.leaveRoom(this.room.roomCode).subscribe();
    this.watchPartyService.disconnect();
    this.router.navigate(['/']);
  }

  private getUsernameFromToken(): string {
    try {
      const token = localStorage.getItem('token');
      if (!token) return '';
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.username || payload.preferred_username || '';
    } catch (e) {
      return '';
    }
  }
}