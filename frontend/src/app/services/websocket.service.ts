import { Injectable, OnDestroy } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import { Observable, Subject } from 'rxjs';

export interface OutputFrame {
  type: 'stdout' | 'stderr' | 'exit' | 'error';
  data?: string;
  exitCode?: number;
  executionTimeMs?: number;
  message?: string;
}

export interface WsExecuteRequest {
  sessionId: string;
  code: string;
  language: string;
  stdin?: string;
}

@Injectable({
  providedIn: 'root'
})
export class WebSocketService implements OnDestroy {

  private client: Client;
  private connected = false;
  private pendingActions: (() => void)[] = [];

  constructor() {
    this.client = new Client({
      brokerURL: `ws://${window.location.host}/ws`,
      reconnectDelay: 3000,
      onConnect: () => {
        this.connected = true;
        this.pendingActions.forEach(fn => fn());
        this.pendingActions = [];
      },
      onDisconnect: () => {
        this.connected = false;
      },
      onStompError: (frame) => {
        console.error('STOMP error', frame);
      }
    });

    this.client.activate();
  }

  execute(request: WsExecuteRequest): Observable<OutputFrame> {
    const subject = new Subject<OutputFrame>();

    const doExecute = () => {
      const sub = this.client.subscribe(
        `/topic/execution/${request.sessionId}/output`,
        (message: IMessage) => {
          const frame: OutputFrame = JSON.parse(message.body);
          subject.next(frame);

          if (frame.type === 'exit' || frame.type === 'error') {
            if (frame.type === 'exit') {
              sub.unsubscribe();
              subject.complete();
            }
          }
        }
      );

      this.client.publish({
        destination: '/app/execute',
        body: JSON.stringify(request)
      });
    };

    if (this.connected) {
      doExecute();
    } else {
      this.pendingActions.push(doExecute);
    }

    return subject.asObservable();
  }

  ngOnDestroy(): void {
    this.client.deactivate();
  }
}
