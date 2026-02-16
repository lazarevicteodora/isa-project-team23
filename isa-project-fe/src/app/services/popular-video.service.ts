import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface PopularVideo {
  videoId: number;
  title: string;
  description: string;
  authorUsername: string;
  thumbnailUrl: string;
  videoUrl: string;
  popularityScore: number;
  rankPosition: number;
  pipelineRunAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class PopularVideoService {

  private apiUrl = `${environment.apiUrl}/popular-videos`;

  constructor(private http: HttpClient) {}

  getPopularVideos(): Observable<PopularVideo[]> {
    return this.http.get<PopularVideo[]>(this.apiUrl);
  }
}