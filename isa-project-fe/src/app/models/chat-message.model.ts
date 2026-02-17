export interface ChatMessage {
  videoId: number;
  username: string;
  message: string;
  timestamp: string;
}

export interface ActiveUsersResponse {
  videoId: number;
  activeUsers: number;
}
