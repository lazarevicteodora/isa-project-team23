import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { RegisterRequest } from '../../models/user.model';

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css']
})
export class RegisterComponent {
  registerData: RegisterRequest = {
    email: '',
    username: '',
    password: '',
    password2: '',
    firstName: '',
    lastName: '',
    address: ''
  };

  errorMessage: string = '';
  successMessage: string = '';
  loading: boolean = false;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  onSubmit(): void {
    this.errorMessage = '';
    this.successMessage = '';

    // Validacija
    if (!this.registerData.email || !this.registerData.username || 
        !this.registerData.password || !this.registerData.password2 ||
        !this.registerData.firstName || !this.registerData.lastName || 
        !this.registerData.address) {
      this.errorMessage = 'Sva polja su obavezna!';
      return;
    }

    if (this.registerData.password !== this.registerData.password2) {
      this.errorMessage = 'Lozinke se ne poklapaju!';
      return;
    }

    if (this.registerData.password.length < 6) {
      this.errorMessage = 'Lozinka mora imati najmanje 6 karaktera!';
      return;
    }

    this.loading = true;

    this.authService.register(this.registerData).subscribe({
      next: (response) => {
        this.loading = false;
        this.successMessage = response;
        // Resetuj formu
        this.registerData = {
          email: '',
          username: '',
          password: '',
          password2: '',
          firstName: '',
          lastName: '',
          address: ''
        };
        // Preusmeri na login nakon 3 sekunde
        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 3000);
      },
      error: (error) => {
        this.loading = false;
        this.errorMessage = error.error || 'Greška pri registraciji!';
      }
    });
  }
}