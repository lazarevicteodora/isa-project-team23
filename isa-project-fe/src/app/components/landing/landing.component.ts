import { Component, OnInit } from '@angular/core';
import { VideoService } from '../../services/video.service';
import { Video } from '../../models/video.model';

@Component({
  selector: 'app-landing',
  templateUrl: './landing.component.html',
  styleUrls: ['./landing.component.css']
})
export class LandingComponent implements OnInit {
  videos: Video[] = [];
  loading: boolean = true;
  errorMessage: string = '';

  constructor(private videoService: VideoService) {}

  ngOnInit(): void {
    this.loadVideos();
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

  toggleVideo(video: Video): void {
    this.videos.forEach(v => {
      if (v !== video) v.showVideo = false;
    });
    video.showVideo = !video.showVideo;
  }

  onImgError(event: any): void {
    event.target.src = 'assets/default-thumbnail.png';
  }
}