package com.arashbox.dto;

public class OutputFrame {

    private String type;
    private String data;
    private Integer exitCode;
    private Long executionTimeMs;
    private String message;

    private OutputFrame() {}

    public static OutputFrame stdout(String data) {
        OutputFrame f = new OutputFrame();
        f.type = "stdout";
        f.data = data;
        return f;
    }

    public static OutputFrame stderr(String data) {
        OutputFrame f = new OutputFrame();
        f.type = "stderr";
        f.data = data;
        return f;
    }

    public static OutputFrame exit(int exitCode, long executionTimeMs) {
        OutputFrame f = new OutputFrame();
        f.type = "exit";
        f.exitCode = exitCode;
        f.executionTimeMs = executionTimeMs;
        return f;
    }

    public static OutputFrame error(String message) {
        OutputFrame f = new OutputFrame();
        f.type = "error";
        f.message = message;
        return f;
    }

    public String getType() { return type; }
    public String getData() { return data; }
    public Integer getExitCode() { return exitCode; }
    public Long getExecutionTimeMs() { return executionTimeMs; }
    public String getMessage() { return message; }
}
