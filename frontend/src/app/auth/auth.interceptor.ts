import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { catchError, switchMap, throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getAccessToken();

  let authReq = req;
  // Exclude refresh endpoint to prevent recursive loop
  if (token && !req.url.includes('/api/auth/refresh')) {
    authReq = req.clone({
      headers: req.headers.set('Authorization', `Bearer ${token}`)
    });
  }

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      // On 401 Unauthorized, attempt to rotate JWT tokens via refresh endpoint
      if (error.status === 401 && !req.url.includes('/api/auth/login') && !req.url.includes('/api/auth/refresh')) {
        return authService.refreshToken().pipe(
          switchMap((res) => {
            const newAuthReq = req.clone({
              headers: req.headers.set('Authorization', `Bearer ${res.accessToken}`)
            });
            return next(newAuthReq);
          }),
          catchError((refreshErr) => {
            authService.logout();
            return throwError(() => refreshErr);
          })
        );
      }
      return throwError(() => error);
    })
  );
};
