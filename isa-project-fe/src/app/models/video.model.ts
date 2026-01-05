export interface Video {
  id: number;
  title: string;
  description: string;
  thumbnailUrl?: string;
  videoUrl?: string;
  showVideo?: boolean;
  viewCount?: number;  
  createdAt: string;       
  authorUsername: string;
  tags: string[];
  latitude?: number;       
  longitude?: number;

}
