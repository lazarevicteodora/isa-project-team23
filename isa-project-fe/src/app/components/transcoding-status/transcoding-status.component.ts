import { Component, Input, OnInit, OnDestroy } from '@angular/core';
import { VideoService } from '../../services/video.service';
import { interval, Subscription } from 'rxjs';

@Component({
  selector: 'app-transcoding-status',
  templateUrl: './transcoding-status.component.html',
  styleUrls: ['./transcoding-status.component.css']
})
export class TranscodingStatusComponent implements OnInit, OnDestroy {
  
  @Input() videoId!: number;
  
  transcodingJobs: any[] = [];
  private pollingSubscription?: Subscription;

  constructor(private videoService: VideoService) {}

  ngOnInit(): void {
    this.loadTranscodingStatus();
    this.startPolling();
  }

  ngOnDestroy(): void {
    this.stopPolling();
  }

  /**
   * Učitaj transcoding status za video
   */
  loadTranscodingStatus(): void {
    this.videoService.getTranscodingStatus(this.videoId).subscribe({
      next: (jobs) => {
        this.transcodingJobs = jobs;
        
        // Stop polling ako su svi završeni
        const hasActive = jobs.some((job: any) => 
          job.status === 'PENDING' || job.status === 'PROCESSING'
        );
        
        if (!hasActive) {
          this.stopPolling();
        }
      },
      error: (err) => {
        console.error('Error loading transcoding status:', err);
      }
    });
  }

  /**
   * Pokreni polling (proveri status svake 5 sekundi)
   */
  startPolling(): void {
    this.pollingSubscription = interval(5000).subscribe(() => {
      this.loadTranscodingStatus();
    });
  }

  /**
   * Zaustavi polling
   */
  stopPolling(): void {
    if (this.pollingSubscription) {
      this.pollingSubscription.unsubscribe();
    }
  }

  /**
   * Retry neuspelog job-a
   */
  retryJob(jobId: string): void {
    if (confirm('Da li želite da pokušate ponovo?')) {
      this.videoService.retryTranscodingJob(jobId).subscribe({
        next: () => {
          alert('Job je ponovo stavljen u red!');
          this.loadTranscodingStatus();
          this.startPolling();
        },
        error: (err) => {
          alert('Greška pri retry-u: ' + err.message);
        }
      });
    }
  }

  /**
   * Ekstraktuj rezoluciju iz putanje fajla
   */
  getResolutionFromPath(path: string): string {
    if (path.includes('720p')) return '720p';
    if (path.includes('480p')) return '480p';
    if (path.includes('360p')) return '360p';
    return 'Unknown';
  }
}