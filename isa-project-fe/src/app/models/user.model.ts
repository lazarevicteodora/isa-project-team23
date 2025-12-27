export interface User {
  id?: number;
  email: string;
  username: string;
  firstName: string;
  lastName: string;
  address: string;
  activated?: boolean;
  enabled?: boolean;
  roles?: Role[];
}
export interface Role {
  id: number;
  name: string;
}
export interface RegisterRequest {
  email: string;
  username: string;
  password: string;
  password2: string;
  firstName: string;
  lastName: string;
  address: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface UserTokenState {
  accessToken: string;
  expiresIn: number;
}