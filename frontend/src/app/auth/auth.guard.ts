import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { filter, map, take } from 'rxjs';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return authService.isInitialAuthChecked$.pipe(
    filter((checked) => checked === true),
    take(1),
    map(() => {
      if (authService.isLoggedIn()) {
        return true;
      } else {
        router.navigate(['/login']);
        return false;
      }
    })
  );
};
