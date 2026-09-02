import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { SessionTimeoutService } from './core/services';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  constructor() {
    inject(SessionTimeoutService).start();
  }
}
