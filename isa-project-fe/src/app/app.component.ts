import { Component, OnInit } from '@angular/core';
import { TestService } from './services/test.service';


@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  title = 'isa-project-fe';
  backendStatus = 'Checking...';

  constructor(private testService: TestService) {}

  ngOnInit(): void {
    // Testiranje konekcije sa backendom
    this.testService.testConnection().subscribe({
      next: (response) => {
        console.log('Backend response:', response);
        this.backendStatus = '✅ Backend is UP!';
      },
      error: (error) => {
        console.error('Backend connection failed:', error);
        this.backendStatus = '❌ Backend connection failed!';
      }
    });
  }
}
