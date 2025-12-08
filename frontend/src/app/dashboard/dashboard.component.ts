import { Component } from '@angular/core';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  template: `
    <div class="dashboard">
      <h2>Ingestion Dashboard</h2>
      <p>Monitor indexing jobs and repository status.</p>
    </div>
  `,
  styles: [`
    .dashboard {
      padding: 1rem;
    }
  `]
})
export class DashboardComponent {
}

