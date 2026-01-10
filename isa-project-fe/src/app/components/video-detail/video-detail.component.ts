import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { VideoService } from '../../services/video.service';
import { Video } from '../../models/video.model';
import { Comment, CommentPage } from '../../models/comment.model';

@Component({
  selector: 'app-video-detail',
  templateUrl: './video-detail.component.html',
  styleUrls: ['./video-detail.component.css']
})
export class VideoDetailComponent implements OnInit {
  video: Video | null = null;
  loading: boolean = true;
  errorMessage: string = '';

  comments: Comment[] = [];
  newComment: string = '';
  currentPage: number = 0;
  pageSize: number = 10;
  totalComments: number = 0;
  totalPages: number = 0;
  loadingComments: boolean = false;
  submittingComment: boolean = false;

  hasLiked: boolean = false;
  likeCount: number = 0;

  isAuthenticated: boolean = false;
  currentUserId: number | null = null;
  currentUserEmail: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private videoService: VideoService
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.params['id']);
    
    this.checkAuthentication();
    this.incrementViewAndLoadVideo(id);
    this.loadComments(id);
    this.loadLikeStatus(id);
  }

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

  incrementViewAndLoadVideo(id: number): void {
    this.videoService.incrementViewCount(id).subscribe({
      next: () => this.loadVideo(id),
      error: () => this.loadVideo(id)
    });
  }

  loadVideo(id: number): void {
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
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = 'Video nije pronađen';
        this.loading = false;
      }
    });
  }

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
          alert(' Prekoračili ste maksimalan broj komentara po satu.');
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
          alert(' Nemate dozvolu da obrišete ovaj komentar.');
        } else {
          alert('Greška pri brisanju komentara.');
        }
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

  showLoginMessage(action: string): void {
    const message = `Morate se prijaviti da biste mogli da ${action} video. Želite li da se prijavite?`;
    if (confirm(message)) {
      this.router.navigate(['/login'], { queryParams: { returnUrl: this.router.url } });
    }
  }

  canDeleteComment(comment: Comment): boolean {
    if (!this.isAuthenticated) {
      return false;
    }
    
    // Proveri po ID-u
    if (this.currentUserId && comment.authorId) {
      return this.currentUserId === comment.authorId;
    }
    
    // Fallback na email
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

  goBack(): void {
    this.router.navigate(['/landing']);
  }
}