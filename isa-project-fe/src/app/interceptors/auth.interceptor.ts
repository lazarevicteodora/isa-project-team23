import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    console.log('🌐 Interceptor activated for:', req.url);
    
const token = localStorage.getItem('token'); 
    console.log('🔑 Token from localStorage:', token);

    if (token) {
      const clonedReq = req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
      console.log('✅ Sending request WITH Authorization header');
      return next.handle(clonedReq);
    }

    console.log('⚠️ Sending request WITHOUT Authorization header');
    return next.handle(req);
  }
}