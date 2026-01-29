import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Video } from '../models/video.model';
import { Comment, CommentPage } from '../models/comment.model';
import { HttpEvent } from '@angular/common/http'; 

@Injectable({
  providedIn: 'root'
})
export class VideoService {

  private apiUrl = 'http://localhost:8080/api/videos';

  constructor(private http: HttpClient) { }

 getAllVideos(): Observable<Video[]> {
  return this.http.get<Video[]>('http://localhost:8080/api/videos');
}


 getThumbnail(videoId: number): Observable<Blob> {
  return this.http.get(
    `${this.apiUrl}/${videoId}/thumbnail`,
    { responseType: 'blob' }
  );
}
  getVideoById(id: number): Observable<Video> {
    return this.http.get<Video>(`${this.apiUrl}/${id}`);
  }
 uploadVideo(formData: FormData): Observable<HttpEvent<Video>> {
  console.log('=== UPLOAD REQUEST ===');
  console.log('FormData entries:');
  formData.forEach((value, key) => {
    if (value instanceof File) {
      console.log(key + ':', value.name, value.size, value.type);
    } else {
      console.log(key + ':', value);
    }
  });
  console.log('Authorization token:', localStorage.getItem('token'));
  console.log('======================');

  return this.http.post<Video>(this.apiUrl, formData, {
    reportProgress: true,  
    observe: 'events'      
  });
}

  incrementViewCount(videoId: number): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${videoId}/view`, {});
  }

  getComments(videoId: number, page: number = 0, size: number = 10): Observable<CommentPage> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<CommentPage>(`${this.apiUrl}/${videoId}/comments`, { params });
  }

  addComment(videoId: number, content: string): Observable<Comment> {
    return this.http.post<Comment>(
      `${this.apiUrl}/${videoId}/comments`,
      { content }
    );
  }

  deleteComment(videoId: number, commentId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${videoId}/comments/${commentId}`);
  }

  getCommentCount(videoId: number): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/${videoId}/comments/count`);
  }

  toggleLike(videoId: number): Observable<{ liked: boolean, likeCount: number }> {
    return this.http.post<{ liked: boolean, likeCount: number }>(
      `${this.apiUrl}/${videoId}/likes`,
      {}
    );
  }

  getLikeCount(videoId: number): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/${videoId}/likes/count`);
  }

  getLikeStatus(videoId: number): Observable<{ liked: boolean }> {
    return this.http.get<{ liked: boolean }>(`${this.apiUrl}/${videoId}/likes/status`);
  }
  
  // Dohvatanje ukupnog broja pregleda preko CRDT-a
getTotalViewsCRDT(videoId: number): Observable<{ totalViews: number }> {
  return this.http.get<{ totalViews: number }>(`${this.apiUrl}/${videoId}/views-crdt`);
}

// Inkrementiranje CRDT brojača pregleda
incrementViewCRDT(videoId: number): Observable<any> {
  return this.http.post(`${this.apiUrl}/${videoId}/view-crdt`, {});
}

}
