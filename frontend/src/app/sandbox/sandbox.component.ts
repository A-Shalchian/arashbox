import { Component, OnInit, OnDestroy, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ExecutionService, ExecutionResponse } from '../services/execution.service';

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
  @ViewChild('outputContainer') outputContainer!: ElementRef;

  editor: any;
  selectedLanguage = 'python';
  isRunning = false;
  output = '';
  executionTime = 0;

  languages = [
    { value: 'python', label: 'Python' },
    { value: 'javascript', label: 'JavaScript' }
  ];

  private readonly defaultCode: Record<string, string> = {
    python: '# Write your Python code here\nprint("Hello, Arashbox!")\n',
    javascript: '// Write your JavaScript code here\nconsole.log("Hello, Arashbox!");\n'
  };

  constructor(private executionService: ExecutionService) {}

  ngOnInit(): void {}

  ngAfterViewInit(): void {
    this.loadMonaco();
  }

  ngOnDestroy(): void {
    this.editor?.dispose();
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

    // Ctrl+Enter to run
    this.editor.addAction({
      id: 'run-code',
      label: 'Run Code',
      keybindings: [monaco.KeyMod.CtrlCmd | monaco.KeyCode.Enter],
      run: () => this.runCode()
    });
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
    this.output = 'Running...\n';

    const code = this.editor.getValue();
    this.executionService.execute({ code, language: this.selectedLanguage }).subscribe({
      next: (res: ExecutionResponse) => {
        this.output = '';
        if (res.stdout) this.output += res.stdout;
        if (res.stderr) this.output += res.stderr;
        if (!res.stdout && !res.stderr) this.output = '(no output)\n';
        this.executionTime = res.executionTimeMs;
        this.isRunning = false;
      },
      error: (err) => {
        this.output = 'Error: Failed to execute code. Is the backend running?\n';
        this.isRunning = false;
      }
    });
  }
}
