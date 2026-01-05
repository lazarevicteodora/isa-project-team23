import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { VideoService } from '../../services/video.service';
import { Video } from '../../models/video.model';

@Component({
  selector: 'app-video-detail',
  templateUrl: './video-detail.component.html',
  styleUrls: ['./video-detail.component.css']
})
export class VideoDetailComponent implements OnInit {
  video: Video | null = null;
  loading: boolean = true;
  errorMessage: string = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private videoService: VideoService
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.params['id']);
    
    this.incrementViewAndLoadVideo(id);
  }

  /**
   * Increment pa učitaj video
   * Ovo garantuje da se pregled broji pri ULASKU na stranicu
   */
  incrementViewAndLoadVideo(id: number): void {
    this.videoService.incrementViewCount(id).subscribe({
      next: () => {
        console.log(`✅ View counted for video ${id}`);
        
        this.loadVideo(id);
      },
      error: (err) => {
        console.error('❌ Greška pri brojanju pregleda:', err);
        
        this.loadVideo(id);
      }
    });
  }

  /**
   * Učitaj video podatke
   */
  loadVideo(id: number): void {
    this.videoService.getVideoById(id).subscribe({
      next: (video) => {
        this.video = video;
        this.loading = false;
        console.log('Video loaded:', video);
      },
      error: (err) => {
        console.error('❌ Greška pri učitavanju videa:', err);
        this.errorMessage = 'Video nije pronađen';
        this.loading = false;
      }
    });
  }

  /**
   * Vrati se na početnu stranicu
   */
  goBack(): void {
    this.router.navigate(['/landing']);
  }
}