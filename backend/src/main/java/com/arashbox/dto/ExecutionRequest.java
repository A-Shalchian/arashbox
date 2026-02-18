package com.arashbox.dto;

import jakarta.validation.constraints.NotBlank;

public class ExecutionRequest {

    @NotBlank
    private String code;

    @NotBlank
    private String language;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
}
