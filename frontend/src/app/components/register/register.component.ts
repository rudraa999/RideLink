import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css']
})
export class RegisterComponent implements OnInit {
  formData = {
    name: '',
    username: '',
    email: '',
    password: '',
    collegeId: null as number | null
  };
  colleges: any[] = [];
  errorMessage: string | null = null;
  successMessage: string | null = null;
  isLoading = false;

  constructor(private authService: AuthService, private router: Router) {
    if (this.authService.isLoggedIn()) {
      this.router.navigate(['/dashboard']);
    }
  }

  ngOnInit(): void {
    this.authService.getColleges().subscribe({
      next: (data) => this.colleges = data,
      error: (err) => this.errorMessage = 'Failed to load colleges. Please reload.'
    });
  }

  onSubmit(): void {
    if (!this.formData.name || !this.formData.username || !this.formData.email || !this.formData.password || !this.formData.collegeId) {
      this.errorMessage = 'Please fill in all details';
      return;
    }

    this.isLoading = true;
    this.errorMessage = null;
    this.successMessage = null;

    this.authService.register(this.formData).subscribe({
      next: () => {
        this.isLoading = false;
        this.successMessage = 'Registration successful! Redirecting to login...';
        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 2000);
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Registration failed. Try again.';
      }
    });
  }
}
