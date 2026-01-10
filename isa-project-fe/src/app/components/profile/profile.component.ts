import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { UserService } from '../../services/user.service';
import { VideoService } from '../../services/video.service';
import { User } from '../../models/user.model';
import { Video } from '../../models/video.model';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css']
})
export class ProfileComponent implements OnInit {
  user: User | null = null;
  videos: Video[] = [];
  loading: boolean = true;
  errorMessage: string = '';
  userId: number = 0;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private userService: UserService,
    private videoService: VideoService
  ) {}

  ngOnInit(): void {
    this.userId = Number(this.route.snapshot.params['id']);
    this.loadUserProfile(this.userId);
    this.loadUserVideos(this.userId);
  }

  loadUserProfile(userId: number): void {
    this.userService.getUserById(userId).subscribe({
      next: (user) => {
        this.user = user;
        this.loading = false;
        console.log('✅ User profile loaded:', user);
      },
      error: (err) => {
        console.error('❌ Greška pri učitavanju profila:', err);
        this.errorMessage = 'Korisnik nije pronađen';
        this.loading = false;
      }
    });
  }

  loadUserVideos(userId: number): void {
    this.videoService.getAllVideos().subscribe({
      next: (allVideos) => {
        // ✅ Filtriraj videe i normalizuj URL-ove
        this.videos = allVideos
          .filter(video => video.authorId === userId)
          .map(video => ({
            ...video,
            thumbnailUrl: this.normalizeUrl(video.thumbnailUrl),
            videoUrl: this.normalizeUrl(video.videoUrl)
          }));
        
        console.log('✅ User videos loaded:', this.videos.length);
        console.log('📸 First video thumbnail:', this.videos[0]?.thumbnailUrl);
      },
      error: (err) => {
        console.error('❌ Greška pri učitavanju videa:', err);
      }
    });
  }

  // ✅ DODAJ normalizaciju URL-a
  private normalizeUrl(url: string | undefined): string {
    if (!url) return '';
    if (url.startsWith('http')) return url;
    return `http://localhost:8080${url}`;
  }

  getTotalViews(): number {
    return this.videos.reduce((total, video) => total + (video.viewCount || 0), 0);
  }

  getTotalLikes(): number {
    return this.videos.reduce((total, video) => total + (video.likeCount || 0), 0);
  }

  getTotalComments(): number {
  return this.videos.reduce((total, video) => total + (video.commentCount || 0), 0);
  }

  goBack(): void {
    this.router.navigate(['/landing']);
  }

  openVideo(videoId: number): void {
    this.router.navigate(['/video', videoId]);
  }

  onImgError(event: any): void {
    console.error('❌ Image load failed for:', event.target.src);
    event.target.src = 'assets/default-thumbnail.png';
  }
}