import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  template: `
    <div class="app-container">
      <header>
        <h1>MegaBrain</h1>
        <p>Code Knowledge Platform</p>
      </header>
      <main>
        <router-outlet></router-outlet>
      </main>
    </div>
  `,
  styles: [`
    .app-container {
      min-height: 100vh;
      display: flex;
      flex-direction: column;
    }
    header {
      background-color: #1976d2;
      color: white;
      padding: 1rem;
      text-align: center;
    }
    header h1 {
      margin: 0;
      font-size: 2rem;
    }
    header p {
      margin: 0.5rem 0 0 0;
      opacity: 0.9;
    }
    main {
      flex: 1;
      padding: 2rem;
    }
  `]
})
export class AppComponent {
  title = 'MegaBrain';
}

