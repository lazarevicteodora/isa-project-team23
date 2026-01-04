import { Component, OnInit } from '@angular/core';
import { VideoService } from '../../services/video.service';
import { Video } from '../../models/video.model';
import { AuthService } from 'src/app/services/auth.service';
import { User } from '../../models/user.model';
import { Router } from '@angular/router';
import { UserService } from 'src/app/services/user.service';
@Component({
  selector: 'app-landing',
  templateUrl: './landing.component.html',
  styleUrls: ['./landing.component.css']
})
export class LandingComponent implements OnInit {
  videos: Video[] = [];
  loading: boolean = true;
  errorMessage: string = '';
  currentUser: User | null = null;

  constructor(
    private videoService: VideoService,
    private userService: UserService, 
    private authService: AuthService,
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
      },
      error: (err) => {
        console.log('Korisnik nije ulogovan (javni pristup)');
        this.currentUser = null;
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
        this.loading = false;
      },
      error: (err) => {
        console.error('Greška pri učitavanju videa', err);
        this.errorMessage = 'Greška pri učitavanju videa';
        this.loading = false;
      }
    });
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