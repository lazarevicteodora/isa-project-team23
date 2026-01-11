export interface Comment {
  id: number;
  content: string;
  videoId: number;
  authorId: number;
  authorUsername: string;
  authorEmail: string;
  createdAt: string;
}

export interface CommentPage {
  content: Comment[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}