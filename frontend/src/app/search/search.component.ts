import { Component } from '@angular/core';

@Component({
  selector: 'app-search',
  standalone: true,
  template: `
    <div class="search">
      <h2>Code Search</h2>
      <p>Search your codebase with semantic understanding.</p>
    </div>
  `,
  styles: [`
    .search {
      padding: 1rem;
    }
  `]
})
export class SearchComponent {
}

