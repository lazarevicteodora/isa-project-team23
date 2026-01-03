import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { UserService } from '../../services/user.service';
import { User } from '../../models/user.model';
import { VideoService } from '../../services/video.service';
import { Video } from '../../models/video.model';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit {
  currentUser: User | null = null;
  loading: boolean = true;
  errorMessage: string = '';
  videos: Video[] = [];

  constructor(
    private authService: AuthService,
    private userService: UserService,
    private videoService: VideoService,
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
        this.loading = false;
      },
      error: (error) => {
        this.loading = false;
        this.errorMessage = 'Greška pri učitavanju korisnika!';
        console.error('Error loading user:', error);
        
        // Ako je 401, preusmeri na login
        if (error.status === 401) {
          this.authService.logout();
        }
      }
    });
  }
  

  loadVideos(): void {
  this.videoService.getAllVideos().subscribe({
    next: (data) => {
      this.videos = data.map(video => ({
        ...video,
        videoUrl: `http://localhost:8080/api/videos/${video.id}/stream`,
        thumbnailUrl: `http://localhost:8080/api/videos/${video.id}/thumbnail`,
        showVideo: false 
      }));
      console.log('VIDEOS WITH URLS:', this.videos); 
    },
    error: (err) => {
      console.error('Greška pri učitavanju videa', err);
      this.errorMessage = 'Greška pri učitavanju videa';
    }
  });
}

toggleVideo(video: any) {
  this.videos.forEach(v => {
    if (v !== video) v.showVideo = false;
  });
  video.showVideo = !video.showVideo;
}

onImgError(event: any) {
  event.target.src = 'assets/default-thumbnail.png'; // fallback slika
}


  logout(): void {
    this.authService.logout();
  }
}