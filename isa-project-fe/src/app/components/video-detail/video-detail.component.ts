import { Component, OnInit, OnDestroy, AfterViewInit, ViewChild, ElementRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { VideoService } from '../../services/video.service';
import { Video } from '../../models/video.model';
import { Comment, CommentPage } from '../../models/comment.model';

@Component({
  selector: 'app-video-detail',
  templateUrl: './video-detail.component.html',
  styleUrls: ['./video-detail.component.css']
})
export class VideoDetailComponent implements OnInit, OnDestroy, AfterViewInit {
  // Video data
  video: Video | null = null;
  loading: boolean = true;
  errorMessage: string = '';
  
  // Scheduled streaming
  streamingStatus: 'UPCOMING' | 'LIVE' | 'NORMAL' = 'NORMAL';
  startsIn: number | null = null;
  currentOffset: number | null = null;
  statusCheckInterval: any;
  playerSetupDone: boolean = false; 

  // Comments
  comments: Comment[] = [];
  newComment: string = '';
  currentPage: number = 0;
  pageSize: number = 10;
  totalComments: number = 0;
  totalPages: number = 0;
  loadingComments: boolean = false;
  submittingComment: boolean = false;
  
  // Likes
  hasLiked: boolean = false;
  likeCount: number = 0;

  // Auth
  isAuthenticated: boolean = false;
  currentUserId: number | null = null;
  currentUserEmail: string | null = null;

  // Video player reference
  @ViewChild('videoPlayer') videoPlayerRef!: ElementRef<HTMLVideoElement>;
  
  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private videoService: VideoService,
    
  ) {}

  // ==================== LIFECYCLE HOOKS ====================
  
  ngOnInit(): void {
    const id = Number(this.route.snapshot.params['id']);
    
    this.checkAuthentication();
    this.incrementViewAndLoadVideo(id);
    this.loadComments(id);
    this.loadLikeStatus(id);
    this.loadStreamingStatus(id);
    
    // Periodično ažuriraj status (svake 3 sekunde)
    this.statusCheckInterval = setInterval(() => {
      this.loadStreamingStatus(id);
    }, 3000);
  }

  ngAfterViewInit(): void {
    // Ako je video već LIVE kad se komponenta učita, setup player
    if (this.streamingStatus === 'LIVE' && this.videoPlayerRef) {
      setTimeout(() => {
        this.setupVideoPlayer();
      }, 500);
    }
  }

  ngOnDestroy(): void {
  // Očisti interval kad korisnik napusti stranicu
  if (this.statusCheckInterval) {
    clearInterval(this.statusCheckInterval);
  }
  
  // Resetuj flag
  this.playerSetupDone = false; // ← DODAJ OVO!
}

  // ==================== SCHEDULED STREAMING ====================

  loadStreamingStatus(videoId: number): void {
    this.videoService.getStreamingStatus(videoId).subscribe({
      next: (status) => {
        const previousStatus = this.streamingStatus;
        
        this.streamingStatus = status.status;
        this.startsIn = status.startsIn;
        this.currentOffset = status.currentOffset;
        
        console.log(`📊 Status: ${status.status}, Offset: ${status.currentOffset}s`);
        
        // Ako je video prešao iz UPCOMING u LIVE, reload-uj video
        if (this.streamingStatus === 'LIVE' && previousStatus === 'UPCOMING') {
          console.log('🔴 Video je sada LIVE! Reload-ujem...');
          this.loadVideoWithCRDTViews(videoId);
          
          // Setup player posle kratkog delay-a (da se učita ViewChild)
          setTimeout(() => {
            this.setupVideoPlayer();
          }, 1000);
        }
        
        // Ako je već LIVE i ima ViewChild, setup player
        if (this.streamingStatus === 'LIVE' && this.videoPlayerRef) {
          this.setupVideoPlayer();
        }
      },
      error: (err) => {
        console.error('Greška pri učitavanju streaming statusa:', err);
      }
    });
  }

setupVideoPlayer(): void {
  if (!this.videoPlayerRef || this.streamingStatus !== 'LIVE' || this.currentOffset === null) {
    return;
  }
  
  // Spreči višestruko pozivanje
  if (this.playerSetupDone) {
    return;
  }
  
  this.playerSetupDone = true;
  console.log('🎬 Setup video player za LIVE streaming...');
  
  const videoElement = this.videoPlayerRef.nativeElement;
  
  // REŠENJE: Proveri da li je video već učitan
  if (videoElement.readyState >= 1) {
    // Video je već spreman, setuj offset ODMAH
    if (this.currentOffset !== null && this.currentOffset > 0) {
      videoElement.currentTime = this.currentOffset;
      console.log(`✅ Offset odmah setovan na: ${this.currentOffset}s (readyState: ${videoElement.readyState})`);
    }
  } else {
    // Video se još učitava, sačekaj event
    console.log('⏳ Čekam da se video učita...');
    
    const onMetadataLoaded = () => {
      if (this.currentOffset !== null && this.currentOffset > 0) {
        videoElement.currentTime = this.currentOffset;
        console.log(`✅ Offset setovan nakon učitavanja: ${this.currentOffset}s`);
      }
      videoElement.removeEventListener('loadedmetadata', onMetadataLoaded);
    };
    
    videoElement.addEventListener('loadedmetadata', onMetadataLoaded);
  }
  
  // Periodično sinhronizuj svake 10 sekundi (samo jednom!)
  setInterval(() => {
    this.syncVideoOffset();
  }, 10000);
}

  syncVideoOffset(): void {
    if (!this.videoPlayerRef || this.streamingStatus !== 'LIVE' || this.currentOffset === null) {
      return;
    }
    
    const videoElement = this.videoPlayerRef.nativeElement;
    
    if (this.video) {
      this.videoService.getStreamingStatus(this.video.id).subscribe({
        next: (status) => {
          if (status.currentOffset !== null && status.currentOffset > 0) {
            const expectedTime = status.currentOffset;
            const actualTime = videoElement.currentTime;
            
            // Ako je razlika > 5 sekundi, re-sync
            if (Math.abs(actualTime - expectedTime) > 5) {
              videoElement.currentTime = expectedTime;
              console.log(`🔄 Video re-synced: ${actualTime}s → ${expectedTime}s`);
            }
          }
        }
      });
    }
  }

  formatTime(seconds: number): string {
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;
    
    if (hours > 0) {
      return `${hours}h ${minutes}m ${secs}s`;
    } else if (minutes > 0) {
      return `${minutes}m ${secs}s`;
    } else {
      return `${secs}s`;
    }
  }

  // ==================== AUTHENTICATION ====================

  checkAuthentication(): void {
    const token = localStorage.getItem('token');
    if (!token) {
      this.isAuthenticated = false;
      return;
    }
    
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const expired = Date.now() >= payload.exp * 1000;
      
      if (expired) {
        localStorage.removeItem('token');
        this.isAuthenticated = false;
      } else {
        this.isAuthenticated = true;
        this.currentUserId = payload.userId || payload.id || payload.user_id || null;
        this.currentUserEmail = payload.sub || payload.email || payload.username || null;
      }
    } catch (e) {
      localStorage.removeItem('token');
      this.isAuthenticated = false;
    }
  }

  // ==================== VIDEO LOADING ====================

  incrementViewAndLoadVideo(id: number): void {
    this.videoService.incrementViewCRDT(id).subscribe({
      next: () => this.loadVideoWithCRDTViews(id),
      error: () => this.loadVideoWithCRDTViews(id)
    });
  }

  loadVideoWithCRDTViews(id: number): void {
    this.videoService.getVideoById(id).subscribe({
      next: (video) => {
        this.video = video;

        if (this.video.tags && typeof this.video.tags === 'string') {
          try { 
            this.video.tags = JSON.parse(this.video.tags as string); 
          } catch (e) { 
            this.video.tags = []; 
          }
        }

        this.likeCount = video.likeCount || 0;

        this.videoService.getTotalViewsCRDT(id).subscribe({
          next: (res) => this.video!.viewCount = res.totalViews,
          error: () => this.video!.viewCount = video.viewCount || 0
        });

        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Video nije pronađen';
        this.loading = false;
      }
    });
  }

  // ==================== COMMENTS ====================

  loadComments(videoId: number): void {
    this.loadingComments = true;
    this.videoService.getComments(videoId, this.currentPage, this.pageSize).subscribe({
      next: (page: CommentPage) => {
        this.comments = (page.content || []).sort((a, b) => {
          return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
        });
        
        this.totalComments = page.totalElements;
        this.totalPages = page.totalPages;
        this.loadingComments = false;
      },
      error: (err) => {
        this.loadingComments = false;
      }
    });
  }

  addComment(): void {
    if (!this.isAuthenticated) {
      this.showLoginMessage('komentarisati');
      return;
    }
    
    if (!this.newComment.trim()) {
      alert('Komentar ne može biti prazan');
      return;
    }
    
    if (!this.video) return;
    
    const videoId = this.video.id;
    const commentText = this.newComment.trim();
    
    this.submittingComment = true;
    
    this.videoService.addComment(videoId, commentText).subscribe({
      next: (comment) => {
        this.newComment = '';
        this.submittingComment = false;
        this.currentPage = 0;
        
        setTimeout(() => {
          this.loadComments(videoId);
        }, 100);
      },
      error: (err) => {
        this.submittingComment = false;
        
        if (err.status === 429) {
          alert('⚠️ Prekoračili ste maksimalan broj komentara po satu.');
        } else if (err.status === 401) {
          alert('Sesija je istekla. Prijavite se ponovo.');
          this.router.navigate(['/login'], { queryParams: { returnUrl: this.router.url } });
        } else {
          alert('Greška pri dodavanju komentara: ' + (err.error?.message || err.message));
        }
      }
    });
  }

  deleteComment(commentId: number): void {
    if (!this.video || !confirm('Da li ste sigurni da želite da obrišete komentar?')) {
      return;
    }
    
    const videoId = this.video.id;
    
    this.videoService.deleteComment(videoId, commentId).subscribe({
      next: () => {
        this.loadComments(videoId);
      },
      error: (err) => {
        if (err.status === 401) {
          alert('🔒 Sesija je istekla.');
          this.router.navigate(['/login'], { queryParams: { returnUrl: this.router.url } });
        } else if (err.status === 403) {
          alert('⚠️ Nemate dozvolu da obrišete ovaj komentar.');
        } else {
          alert('Greška pri brisanju komentara.');
        }
      }
    });
  }

  canDeleteComment(comment: Comment): boolean {
    if (!this.isAuthenticated) {
      return false;
    }
    
    if (this.currentUserId && comment.authorId) {
      return this.currentUserId === comment.authorId;
    }
    
    if (this.currentUserEmail && comment.authorEmail) {
      return this.currentUserEmail.toLowerCase() === comment.authorEmail.toLowerCase();
    }
    
    return false;
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages - 1 && this.video) {
      this.currentPage++;
      this.loadComments(this.video.id);
    }
  }

  previousPage(): void {
    if (this.currentPage > 0 && this.video) {
      this.currentPage--;
      this.loadComments(this.video.id);
    }
  }

  // ==================== LIKES ====================

  loadLikeStatus(videoId: number): void {
    if (!this.isAuthenticated) {
      this.hasLiked = false;
      return;
    }

    this.videoService.getLikeStatus(videoId).subscribe({
      next: (status) => {
        this.hasLiked = status.liked;
      },
      error: (err) => {
        console.error('Greška pri učitavanju like statusa:', err);
      }
    });
  }

  toggleLike(): void {
    if (!this.isAuthenticated) {
      this.showLoginMessage('lajkovati');
      return;
    }
    
    if (!this.video) return;
    
    this.videoService.toggleLike(this.video.id).subscribe({
      next: (response) => {
        this.hasLiked = response.liked;
        this.likeCount = response.likeCount;
      },
      error: (err) => {
        if (err.status === 401) {
          alert('🔒 Sesija je istekla.');
          this.router.navigate(['/login'], { queryParams: { returnUrl: this.router.url } });
        } else {
          alert('Greška pri lajkovanju.');
        }
      }
    });
  }

  // ==================== HELPERS ====================

  showLoginMessage(action: string): void {
    const message = `Morate se prijaviti da biste mogli ${action} video. Želite li da se prijavite?`;
    if (confirm(message)) {
      this.router.navigate(['/login'], { queryParams: { returnUrl: this.router.url } });
    }
  }

  goBack(): void {
    this.router.navigate(['/landing']);
  }
}