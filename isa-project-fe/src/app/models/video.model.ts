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
  authorId?: number;
  tags: string[];
  latitude?: number;       
  longitude?: number;
  likeCount?: number;
  commentCount?: number;
}
