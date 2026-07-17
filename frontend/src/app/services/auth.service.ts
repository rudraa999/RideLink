import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8080/api';
  private accessToken: string | null = null;
  private currentUserSubject = new BehaviorSubject<any>(null);
  public currentUser$ = this.currentUserSubject.asObservable();
  private isInitialAuthCheckedSubject = new BehaviorSubject<boolean>(false);
  public isInitialAuthChecked$ = this.isInitialAuthCheckedSubject.asObservable();

  constructor(private http: HttpClient, private router: Router) {
    // Attempt auto-login on startup by refreshing token
    this.tryAutoLogin();
  }

  public getAccessToken(): string | null {
    return this.accessToken;
  }

  public isLoggedIn(): boolean {
    return this.accessToken !== null;
  }

  public getCurrentUser(): any {
    return this.currentUserSubject.value;
  }

  public getColleges(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/colleges`);
  }

  public register(payload: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/register`, payload);
  }

  public login(payload: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/auth/login`, payload, { withCredentials: true }).pipe(
      tap((res) => {
        this.accessToken = res.accessToken;
        this.currentUserSubject.next({
          username: res.username,
          name: res.name,
          collegeName: res.collegeName
        });
      })
    );
  }

  public refreshToken(): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/auth/refresh`, {}, { withCredentials: true }).pipe(
      tap((res) => {
        this.accessToken = res.accessToken;
        this.currentUserSubject.next({
          username: res.username,
          name: res.name,
          collegeName: res.collegeName
        });
      })
    );
  }

  public logout(): void {
    this.http.post(`${this.apiUrl}/auth/logout`, {}, { withCredentials: true }).subscribe({
      next: () => this.clearSession(),
      error: () => this.clearSession()
    });
  }

  private clearSession(): void {
    this.accessToken = null;
    this.currentUserSubject.next(null);
    this.router.navigate(['/login']);
  }

  public getUserProfile(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/users/me`);
  }

  private tryAutoLogin(): void {
    this.refreshToken().subscribe({
      next: () => {
        console.log('Auto-login successful');
        this.isInitialAuthCheckedSubject.next(true);
      },
      error: () => {
        console.log('No active session found');
        this.isInitialAuthCheckedSubject.next(true);
      }
    });
  }
}
