import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { LoginRequest, RegisterRequest, UserTokenState } from '../models/user.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = environment.apiUrl;
  private tokenKey = 'jwt_token';
  
  // BehaviorSubject za praćenje stanja autentifikacije
  private isAuthenticatedSubject = new BehaviorSubject<boolean>(this.hasToken());
  public isAuthenticated$ = this.isAuthenticatedSubject.asObservable();

  constructor(
    private http: HttpClient,
    private router: Router
  ) {}

  /**
   * Registracija novog korisnika
   */
  register(registerData: RegisterRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/register`, registerData, {
      responseType: 'text'
    });
  }

  /**
   * Login korisnika
   */
  login(loginData: LoginRequest): Observable<UserTokenState> {
    return this.http.post<UserTokenState>(`${this.apiUrl}/auth/login`, loginData)
      .pipe(
        tap(response => {
          // Sačuvaj token u localStorage
          this.setToken(response.accessToken);
          this.isAuthenticatedSubject.next(true);
        })
      );
  }

  /**
   * Aktivacija naloga preko tokena
   */
  activateAccount(token: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/auth/activate/${token}`, {
      responseType: 'text'
    });
  }

  /**
   * Logout korisnika
   */
  logout(): void {
    this.removeToken();
    this.isAuthenticatedSubject.next(false);
    this.router.navigate(['/login']);
  }

  /**
   * Sačuvaj JWT token u localStorage
   */
  private setToken(token: string): void {
    localStorage.setItem(this.tokenKey, token);
  }

  /**
   * Preuzmi JWT token iz localStorage
   */
  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  /**
   * Obriši JWT token iz localStorage
   */
  private removeToken(): void {
    localStorage.removeItem(this.tokenKey);
  }

  /**
   * Proveri da li korisnik ima token
   */
  hasToken(): boolean {
    return !!this.getToken();
  }

  /**
   * Proveri da li je korisnik ulogovan
   */
  isLoggedIn(): boolean {
    return this.hasToken();
  }
}