package com.arashbox.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class WsExecuteRequest {

    @NotBlank
    private String sessionId;

    @NotBlank
    @Size(max = 65_536)
    private String code;

    @NotBlank
    private String language;

    @Size(max = 65_536)
    private String stdin;

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getStdin() { return stdin; }
    public void setStdin(String stdin) { this.stdin = stdin; }
}
