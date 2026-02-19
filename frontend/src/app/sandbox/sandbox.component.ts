import { Component, OnInit, OnDestroy, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { ExecutionService, ExecutionResponse } from '../services/execution.service';
import { WebSocketService, OutputFrame } from '../services/websocket.service';
import { Terminal } from '@xterm/xterm';
import { FitAddon } from '@xterm/addon-fit';

declare const monaco: any;

@Component({
  selector: 'app-sandbox',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './sandbox.component.html',
  styleUrl: './sandbox.component.scss'
})
export class SandboxComponent implements OnInit, AfterViewInit, OnDestroy {

  @ViewChild('editorContainer') editorContainer!: ElementRef;
  @ViewChild('terminalContainer') terminalContainer!: ElementRef;

  editor: any;
  selectedLanguage = 'python';
  isRunning = false;
  stdinInput = '';
  executionTime = 0;
  exitCode: number | null = null;

  languages = [
    { value: 'python', label: 'Python' },
    { value: 'javascript', label: 'JavaScript' }
  ];

  private readonly defaultCode: Record<string, string> = {
    python: '# Write your Python code here\nprint("Hello, Arashbox!")\n',
    javascript: '// Write your JavaScript code here\nconsole.log("Hello, Arashbox!");\n'
  };

  private terminal!: Terminal;
  private fitAddon!: FitAddon;
  private executionSub?: Subscription;
  private resizeObserver?: ResizeObserver;

  constructor(
    private executionService: ExecutionService,
    private wsService: WebSocketService
  ) {}

  ngOnInit(): void {}

  ngAfterViewInit(): void {
    this.loadMonaco();
    this.initTerminal();
  }

  ngOnDestroy(): void {
    this.editor?.dispose();
    this.terminal?.dispose();
    this.executionSub?.unsubscribe();
    this.resizeObserver?.disconnect();
  }

  private loadMonaco(): void {
    const onGo = () => {
      (window as any).require.config({
        paths: { vs: 'assets/monaco/vs' }
      });
      (window as any).require(['vs/editor/editor.main'], () => {
        this.initEditor();
      });
    };

    if ((window as any).require) {
      onGo();
      return;
    }

    const script = document.createElement('script');
    script.src = 'assets/monaco/vs/loader.js';
    script.onload = onGo;
    document.body.appendChild(script);
  }

  private initEditor(): void {
    this.editor = monaco.editor.create(this.editorContainer.nativeElement, {
      value: this.defaultCode[this.selectedLanguage],
      language: this.selectedLanguage,
      theme: 'vs-dark',
      fontSize: 14,
      fontFamily: "'JetBrains Mono', monospace",
      minimap: { enabled: false },
      automaticLayout: true,
      scrollBeyondLastLine: false,
      padding: { top: 12 },
      lineNumbers: 'on',
      renderLineHighlight: 'line',
      tabSize: 2
    });

    this.editor.addAction({
      id: 'run-code',
      label: 'Run Code',
      keybindings: [monaco.KeyMod.CtrlCmd | monaco.KeyCode.Enter],
      run: () => this.runCode()
    });
  }

  private initTerminal(): void {
    this.terminal = new Terminal({
      theme: {
        background: '#181825',
        foreground: '#cdd6f4',
        cursor: '#cdd6f4',
        red: '#f38ba8',
        green: '#a6e3a1',
        yellow: '#f9e2af',
        blue: '#89b4fa',
      },
      fontFamily: "'JetBrains Mono', monospace",
      fontSize: 13,
      lineHeight: 1.4,
      cursorBlink: false,
      disableStdin: true,
      convertEol: true,
    });

    this.fitAddon = new FitAddon();
    this.terminal.loadAddon(this.fitAddon);
    this.terminal.open(this.terminalContainer.nativeElement);

    setTimeout(() => this.fitAddon.fit(), 0);

    this.resizeObserver = new ResizeObserver(() => {
      try { this.fitAddon.fit(); } catch {}
    });
    this.resizeObserver.observe(this.terminalContainer.nativeElement);
  }

  onLanguageChange(): void {
    if (this.editor) {
      const model = this.editor.getModel();
      monaco.editor.setModelLanguage(model, this.selectedLanguage);

      const currentCode = this.editor.getValue();
      const isDefault = Object.values(this.defaultCode).some(d => d.trim() === currentCode.trim());
      if (isDefault || !currentCode.trim()) {
        this.editor.setValue(this.defaultCode[this.selectedLanguage]);
      }
    }
  }

  runCode(): void {
    if (this.isRunning) return;

    this.isRunning = true;
    this.executionTime = 0;
    this.exitCode = null;
    this.terminal.reset();

    const code = this.editor.getValue();
    const stdin = this.stdinInput || undefined;
    const sessionId = crypto.randomUUID();

    this.executionSub = this.wsService.execute({ sessionId, code, language: this.selectedLanguage, stdin }).subscribe({
      next: (frame: OutputFrame) => {
        switch (frame.type) {
          case 'stdout':
            this.terminal.write(frame.data ?? '');
            break;
          case 'stderr':
            this.terminal.write(`\x1b[31m${frame.data ?? ''}\x1b[0m`);
            break;
          case 'exit':
            this.executionTime = frame.executionTimeMs ?? 0;
            this.exitCode = frame.exitCode ?? 0;
            this.isRunning = false;
            break;
          case 'error':
            this.terminal.write(`\x1b[31m${frame.message ?? ''}\x1b[0m`);
            break;
        }
      },
      error: () => {
        this.runCodeRest(code, stdin);
      },
      complete: () => {
        this.isRunning = false;
      }
    });
  }

  private runCodeRest(code: string, stdin?: string): void {
    this.terminal.reset();
    this.executionService.execute({ code, language: this.selectedLanguage, stdin }).subscribe({
      next: (res: ExecutionResponse) => {
        if (res.stdout) this.terminal.write(res.stdout);
        if (res.stderr) this.terminal.write(`\x1b[31m${res.stderr}\x1b[0m`);
        if (!res.stdout && !res.stderr) this.terminal.write('(no output)');
        this.executionTime = res.executionTimeMs;
        this.exitCode = res.exitCode;
        this.isRunning = false;
      },
      error: () => {
        this.terminal.write('\x1b[31mError: Failed to execute code. Is the backend running?\x1b[0m');
        this.isRunning = false;
      }
    });
  }
}
