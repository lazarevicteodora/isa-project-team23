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
  isScheduled?: boolean;
  scheduledFor?: string;  // ISO string (2026-02-15T20:30:00)
  streamingStatus?: 'UPCOMING' | 'LIVE' | null;  // status videa
  currentOffset?: number;  // u sekundama - offset od početka
  transcodingStatus?: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  transcodingProgress?: number; // 0-100
}
