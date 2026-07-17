import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { RideService } from '../../services/ride.service';
import { Subscription, interval, startWith, switchMap } from 'rxjs';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit, OnDestroy {
  userProfile: any = null;
  isActive = false;
  availability: any = null;
  
  compatibleMatches: any[] = [];
  incomingRequests: any[] = [];
  outgoingRequests: any[] = [];
  
  activeMatch: any = null;
  chatMessages: any[] = [];
  newMessage = '';
  
  // Toggle status form
  toggleForm = {
    destination: '',
    transportType: 'ANY'
  };

  private pollSubscription: Subscription | null = null;
  private chatSubscription: Subscription | null = null;

  constructor(
    public authService: AuthService,
    private rideService: RideService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // 1. Fetch user profile
    this.authService.getUserProfile().subscribe({
      next: (profile) => this.userProfile = profile,
      error: () => this.authService.logout()
    });

    // 2. Setup periodic polling (every 5 seconds) to fetch active states, matches, and requests
    this.pollSubscription = interval(5000)
      .pipe(
        startWith(0),
        switchMap(() => this.rideService.getActiveMatch())
      )
      .subscribe({
        next: (match) => {
          const previousMatch = this.activeMatch;
          this.activeMatch = match;
          
          if (match) {
            // If we just got a new match, connect to websocket chat
            if (!previousMatch || previousMatch.id !== match.id) {
              this.initChat(match.id);
            }
          } else {
            // No active match, ensure we disconnect websocket if we were connected
            this.rideService.disconnectFromChat();
            this.chatMessages = [];
          }
          this.loadDashboardData();
        },
        error: () => {
          this.activeMatch = null;
          this.rideService.disconnectFromChat();
          this.chatMessages = [];
          this.loadDashboardData();
        }
      });

    // 3. Listen to incoming WebSocket chat messages
    this.chatSubscription = this.rideService.messages$.subscribe((msg) => {
      // Avoid duplicates
      if (!this.chatMessages.some((m) => m.id === msg.id)) {
        this.chatMessages.push(msg);
        this.scrollToBottom();
      }
    });
  }

  ngOnDestroy(): void {
    if (this.pollSubscription) {
      this.pollSubscription.unsubscribe();
    }
    if (this.chatSubscription) {
      this.chatSubscription.unsubscribe();
    }
    this.rideService.disconnectFromChat();
  }

  private loadDashboardData(): void {
    // Load current availability status
    this.rideService.getAvailabilityMe().subscribe({
      next: (res) => {
        this.availability = res;
        this.isActive = res.active;
        if (this.isActive) {
          this.loadCompatibleMatches();
        } else {
          this.compatibleMatches = [];
        }
      },
      error: () => {
        this.isActive = false;
        this.availability = null;
      }
    });

    // Load incoming/outgoing requests
    this.rideService.getIncomingRequests().subscribe((data) => this.incomingRequests = data);
    this.rideService.getOutgoingRequests().subscribe((data) => this.outgoingRequests = data);
  }

  private loadCompatibleMatches(): void {
    this.rideService.getCompatibleMatches().subscribe((data) => this.compatibleMatches = data);
  }

  private initChat(matchId: number): void {
    // Load historical messages first
    this.rideService.getChatHistory(matchId).subscribe((history) => {
      this.chatMessages = history;
      this.scrollToBottom();
    });
    // Establish STOMP connection
    this.rideService.connectToChat(matchId);
  }

  // Availability Actions
  public handleToggle(): void {
    if (this.isActive) {
      // Toggling off
      this.rideService.deactivateAvailability().subscribe(() => this.loadDashboardData());
    } else {
      // Form values require inputs first. We will handle registration submission inside HTML.
    }
  }

  public activateStatus(): void {
    if (!this.toggleForm.destination) {
      alert('Please select a destination.');
      return;
    }

    const payload = {
      destination: this.toggleForm.destination,
      transportType: this.toggleForm.transportType
    };

    this.rideService.setAvailability(payload).subscribe({
      next: () => {
        this.loadDashboardData();
        // Clear form
        this.toggleForm.destination = '';
      },
      error: (err) => alert(err.error?.message || 'Failed to toggle active status')
    });
  }

  // Request Actions
  public sendRideRequest(coTravellerId: number): void {
    this.rideService.sendRequest(coTravellerId).subscribe({
      next: () => {
        this.loadDashboardData();
        alert('Ride sharing request sent!');
      },
      error: (err) => alert(err.error?.message || 'Failed to send request')
    });
  }

  public acceptRideRequest(requestId: number): void {
    this.rideService.acceptRequest(requestId).subscribe({
      next: () => this.loadDashboardData(),
      error: (err) => alert(err.error?.message || 'Failed to accept request')
    });
  }

  public rejectRideRequest(requestId: number): void {
    this.rideService.rejectRequest(requestId).subscribe({
      next: () => this.loadDashboardData(),
      error: (err) => alert(err.error?.message || 'Failed to reject request')
    });
  }

  public cancelRideRequest(requestId: number): void {
    this.rideService.cancelRequest(requestId).subscribe({
      next: () => this.loadDashboardData(),
      error: (err) => alert(err.error?.message || 'Failed to cancel request')
    });
  }

  // Match Actions
  public completeActiveMatch(matchId: number): void {
    this.rideService.completeMatch(matchId).subscribe({
      next: () => this.loadDashboardData(),
      error: (err) => alert(err.error?.message || 'Failed to complete match')
    });
  }

  public cancelActiveMatch(matchId: number): void {
    this.rideService.cancelMatch(matchId).subscribe({
      next: () => this.loadDashboardData(),
      error: (err) => alert(err.error?.message || 'Failed to cancel match')
    });
  }

  // Chat Actions
  public sendChat(): void {
    if (!this.newMessage.trim() || !this.activeMatch) {
      return;
    }
    this.rideService.sendMessage(this.activeMatch.id, this.newMessage);
    this.newMessage = '';
  }

  public getOtherParticipant(match: any): any {
    if (!match || !match.participants) return null;
    return match.participants.find((p: any) => p.username !== this.userProfile?.username);
  }

  private scrollToBottom(): void {
    setTimeout(() => {
      const container = document.getElementById('chat-messages-container');
      if (container) {
        container.scrollTop = container.scrollHeight;
      }
    }, 100);
  }

  public logout(): void {
    this.authService.logout();
  }
}
