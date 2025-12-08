import { Component } from '@angular/core';

@Component({
  selector: 'app-chat',
  standalone: true,
  template: `
    <div class="chat">
      <h2>RAG Chat</h2>
      <p>Ask questions about your codebase.</p>
    </div>
  `,
  styles: [`
    .chat {
      padding: 1rem;
    }
  `]
})
export class ChatComponent {
}

