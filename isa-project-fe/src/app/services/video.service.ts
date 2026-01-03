import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Video } from '../models/video.model';
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
 
  uploadVideo(formData: FormData): Observable<HttpEvent<Video>> {
    return this.http.post<Video>(this.apiUrl, formData, {
      reportProgress: true,  
      observe: 'events'      
    });
  }
}
