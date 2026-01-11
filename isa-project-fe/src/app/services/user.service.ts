import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { User } from '../models/user.model';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  /**
   * Dobavi trenutno ulogovanog korisnika
   */
  getCurrentUser(): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/user/whoami`);
  }

  /**
   * Dobavi sve korisnike (samo za ADMIN)
   */
  getAllUsers(): Observable<User[]> {
    return this.http.get<User[]>(`${this.apiUrl}/users/all`);
  }

  getUserById(userId: number): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/user/${userId}`);
  }
}