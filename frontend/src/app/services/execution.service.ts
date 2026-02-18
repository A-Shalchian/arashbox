import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ExecutionRequest {
  code: string;
  language: string;
}

export interface ExecutionResponse {
  stdout: string;
  stderr: string;
  exitCode: number;
  executionTimeMs: number;
}

@Injectable({
  providedIn: 'root'
})
export class ExecutionService {

  constructor(private http: HttpClient) {}

  execute(request: ExecutionRequest): Observable<ExecutionResponse> {
    return this.http.post<ExecutionResponse>('/api/execute', request);
  }
}
