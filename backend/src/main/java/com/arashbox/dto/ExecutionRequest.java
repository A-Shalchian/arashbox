package com.arashbox.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ExecutionRequest {

    @NotBlank
    @Size(max = 65_536, message = "Code must not exceed 64KB")
    private String code;

    @NotBlank
    private String language;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
}
