import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent {
  formData = {
    usernameOrEmail: '',
    password: ''
  };
  errorMessage: string | null = null;
  isLoading = false;

  constructor(private authService: AuthService, private router: Router) {
    // If already logged in, redirect to dashboard
    if (this.authService.isLoggedIn()) {
      this.router.navigate(['/dashboard']);
    }
  }

  onSubmit(): void {
    if (!this.formData.usernameOrEmail || !this.formData.password) {
      this.errorMessage = 'Please enter all credentials';
      return;
    }

    this.isLoading = true;
    this.errorMessage = null;

    this.authService.login(this.formData).subscribe({
      next: () => {
        this.isLoading = false;
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Invalid username/email or password';
      }
    });
  }
}
