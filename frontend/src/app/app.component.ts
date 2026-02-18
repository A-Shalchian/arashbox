import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  template: `
    <header class="header">
      <div class="logo">arashbox</div>
    </header>
    <main>
      <router-outlet />
    </main>
  `,
  styles: [`
    .header {
      height: 48px;
      background: var(--bg-secondary);
      border-bottom: 1px solid var(--border);
      display: flex;
      align-items: center;
      padding: 0 16px;
    }
    .logo {
      font-family: var(--font-mono);
      font-weight: 700;
      font-size: 18px;
      color: var(--accent);
    }
    main {
      height: calc(100vh - 48px);
    }
  `]
})
export class AppComponent {}
