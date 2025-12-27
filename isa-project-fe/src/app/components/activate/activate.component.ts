import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-activate',
  templateUrl: './activate.component.html',
  styleUrls: ['./activate.component.css']
})
export class ActivateComponent implements OnInit {
  loading: boolean = true;
  successMessage: string = '';
  errorMessage: string = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    // Preuzmi token iz URL parametara
    const token = this.route.snapshot.paramMap.get('token');
    
    if (token) {
      this.activateAccount(token);
    } else {
      this.loading = false;
      this.errorMessage = 'Nevažeći aktivacioni link!';
    }
  }

  activateAccount(token: string): void {
    this.authService.activateAccount(token).subscribe({
      next: (response) => {
        this.loading = false;
        this.successMessage = response;
        
        // Preusmeri na login nakon 3 sekunde
        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 3000);
      },
      error: (error) => {
        this.loading = false;
        this.errorMessage = error.error || 'Greška pri aktivaciji naloga!';
      }
    });
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }
}