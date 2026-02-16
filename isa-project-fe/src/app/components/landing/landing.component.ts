import { Component, OnInit } from '@angular/core';
import { VideoService } from '../../services/video.service';
import { Video } from '../../models/video.model';
import { AuthService } from 'src/app/services/auth.service';
import { User } from '../../models/user.model';
import { Router } from '@angular/router';
import { UserService } from 'src/app/services/user.service';
import { PopularVideoService, PopularVideo } from '../../services/popular-video.service';

@Component({
  selector: 'app-landing',
  templateUrl: './landing.component.html',
  styleUrls: ['./landing.component.css']
})
export class LandingComponent implements OnInit {
  videos: Video[] = [];
  popularVideos: PopularVideo[] = [];
  loading: boolean = true;
  errorMessage: string = '';
  currentUser: User | null = null;

  constructor(
    private videoService: VideoService,
    private userService: UserService, 
    private authService: AuthService,
    private popularVideoService: PopularVideoService,
    private router: Router
  ) {}
  
  ngOnInit(): void {
    this.loadCurrentUser();
    this.loadVideos();
  }

  loadCurrentUser(): void {
    this.userService.getCurrentUser().subscribe({
      next: (user) => {
        this.currentUser = user;
        // Učitaj popularne videe samo ako je korisnik ulogovan
        this.loadPopularVideos();
      },
      error: (err) => {
        console.log('Korisnik nije ulogovan (javni pristup)');
        this.currentUser = null;
      }
    });
  }

  loadPopularVideos(): void {
    this.popularVideoService.getPopularVideos().subscribe({
      next: (data) => {
        this.popularVideos = data.map(v => ({
          ...v,
          thumbnailUrl: this.normalizeUrl(v.thumbnailUrl),
          videoUrl: this.normalizeUrl(v.videoUrl)
        }));
        console.log('Popularni videi učitani:', this.popularVideos.length);
      },
      error: (err) => {
        console.error('Greška pri učitavanju popularnih videa:', err);
      }
    });
  }

  loadVideos(): void {
    this.videoService.getAllVideos().subscribe({
      next: (data) => {
        this.videos = data.map(video => ({
          ...video,
          thumbnailUrl: this.normalizeUrl(video.thumbnailUrl),
          videoUrl: this.normalizeUrl(video.videoUrl)
        }));
        this.loading = false;
      },
      error: (err) => {
        console.error('Greška pri učitavanju videa', err);
        this.errorMessage = 'Greška pri učitavanju videa';
        this.loading = false;
      }
    });
  }

  private normalizeUrl(url: string | undefined): string {
    if (!url) return '';
    if (url.startsWith('http')) return url;
    return `http://localhost:8080${url}`;
  }

  trackByVideoId(index: number, video: Video): number {
    return video.id;
  }

  onImgError(event: any): void {
    event.target.src = 'assets/default-thumbnail.png';
  }

  logout(): void {
    this.authService.logout();
    this.currentUser = null;
    this.router.navigate(['/']);
  }
}