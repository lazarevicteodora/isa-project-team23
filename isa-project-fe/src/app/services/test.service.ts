import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class TestService {

  constructor(private http: HttpClient) { }

  testConnection(): Observable<any> {
    return this.http.get('http://localhost:8080/api/test/health');
  }
}