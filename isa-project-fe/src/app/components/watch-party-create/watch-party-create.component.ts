import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { WatchPartyService } from '../../services/watch-party.service';
import { VideoService } from '../../services/video.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-watch-party-create',
  templateUrl: './watch-party-create.component.html',
  styleUrls: ['./watch-party-create.component.css']
})
export class WatchPartyCreateComponent implements OnInit {

  videoId: number = 0;
  videoTitle: string = '';
  loading: boolean = false;
  errorMessage: string = '';
  joinCode: string = '';
  joiningRoom: boolean = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private watchPartyService: WatchPartyService,
    private videoService: VideoService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    // ✅ Provera autentifikacije
    if (!this.authService.isLoggedIn()) {
      console.error('User not authenticated');
      this.errorMessage = 'Morate biti prijavljeni da biste kreirali sobu!';
      setTimeout(() => {
        this.router.navigate(['/login'], { 
          queryParams: { returnUrl: this.router.url } 
        });
      }, 2000);
      return;
    }

    this.videoId = Number(this.route.snapshot.params['videoId']);
    
    if (!this.videoId || isNaN(this.videoId)) {
      this.errorMessage = 'Neispravan ID videa!';
      return;
    }

    this.loadVideoTitle();
  }

  loadVideoTitle(): void {
    this.videoService.getVideoById(this.videoId).subscribe({
      next: (video) => {
        this.videoTitle = video.title;
        console.log('Video loaded:', this.videoTitle);
      },
      error: (err) => {
        console.error('Error loading video:', err);
        this.errorMessage = 'Video nije pronađen!';
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/video', this.videoId]);
  }

  createRoom(): void {
    // ✅ Dodatne provere
    if (!this.authService.isLoggedIn()) {
      this.errorMessage = 'Morate biti prijavljeni!';
      this.router.navigate(['/login']);
      return;
    }

    if (!this.videoId || !this.videoTitle) {
      this.errorMessage = 'Video podaci nisu učitani!';
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    console.log('Creating room for video:', this.videoId, this.videoTitle);
    console.log('Token exists:', !!localStorage.getItem('token'));

    this.watchPartyService.createRoom(this.videoId, this.videoTitle).subscribe({
      next: (room) => {
        console.log('Room created successfully:', room);
        this.loading = false;
        // Preusmeriti na sobu
        this.router.navigate(['/watch-party', room.roomCode]);
      },
      error: (err) => {
        console.error('Error creating room:', err);
        this.loading = false;
        
        // ✅ Detaljnije poruke o greškama
        if (err.status === 401) {
          this.errorMessage = 'Niste prijavljeni! Prijavite se ponovo.';
          setTimeout(() => {
            this.router.navigate(['/login'], { 
              queryParams: { returnUrl: this.router.url } 
            });
          }, 2000);
        } else if (err.status === 400) {
          this.errorMessage = 'Neispravni podaci: ' + (err.error?.error || err.error?.message || 'Nepoznata greška');
        } else if (err.status === 500) {
          this.errorMessage = 'Greška na serveru: ' + (err.error?.error || err.error?.message || 'Pokušajte ponovo');
        } else {
          this.errorMessage = 'Greška pri kreiranju sobe: ' + (err.error?.error || err.error?.message || err.message);
        }
      }
    });
  }

  joinRoom(): void {
    if (!this.joinCode.trim()) {
      this.errorMessage = 'Unesite kod sobe!';
      return;
    }

    // ✅ Provera autentifikacije
    if (!this.authService.isLoggedIn()) {
      this.errorMessage = 'Morate biti prijavljeni da biste se pridružili sobi!';
      setTimeout(() => {
        this.router.navigate(['/login']);
      }, 2000);
      return;
    }

    this.joiningRoom = true;
    this.errorMessage = '';

    const code = this.joinCode.trim().toUpperCase();
    console.log('Joining room:', code);
    
    this.router.navigate(['/watch-party', code]);
  }
}