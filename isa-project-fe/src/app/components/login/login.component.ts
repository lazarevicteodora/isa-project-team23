import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { LoginRequest } from '../../models/user.model';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent {
  loginData: LoginRequest = {
    email: '',
    password: ''
  };

  errorMessage: string = '';
  loading: boolean = false;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  onSubmit(): void {
    this.errorMessage = '';

    // Validacija
    if (!this.loginData.email || !this.loginData.password) {
      this.errorMessage = 'Email i lozinka su obavezni!';
      return;
    }

    this.loading = true;

    this.authService.login(this.loginData).subscribe({
      next: (response) => {
        this.loading = false;
        console.log('Login successful!', response);
        // Preusmeri na home stranicu nakon uspešnog login-a
        this.router.navigate(['/home']);
      },
      error: (error) => {
        this.loading = false;
        this.errorMessage = error.error || 'Pogrešan email ili lozinka!';
        console.error('Login error:', error);
      }
    });
  }
}