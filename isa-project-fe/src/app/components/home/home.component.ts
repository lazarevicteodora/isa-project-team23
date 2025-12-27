import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { UserService } from '../../services/user.service';
import { User } from '../../models/user.model';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit {
  currentUser: User | null = null;
  loading: boolean = true;
  errorMessage: string = '';

  constructor(
    private authService: AuthService,
    private userService: UserService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadCurrentUser();
  }

  loadCurrentUser(): void {
    this.userService.getCurrentUser().subscribe({
      next: (user) => {
        this.currentUser = user;
        this.loading = false;
      },
      error: (error) => {
        this.loading = false;
        this.errorMessage = 'Greška pri učitavanju korisnika!';
        console.error('Error loading user:', error);
        
        // Ako je 401, preusmeri na login
        if (error.status === 401) {
          this.authService.logout();
        }
      }
    });
  }

  logout(): void {
    this.authService.logout();
  }
}