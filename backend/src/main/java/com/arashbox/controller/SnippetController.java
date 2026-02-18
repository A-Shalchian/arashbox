package com.arashbox.controller;

import com.arashbox.model.Snippet;
import com.arashbox.service.SnippetService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/snippets")
public class SnippetController {

    private final SnippetService snippetService;

    public SnippetController(SnippetService snippetService) {
        this.snippetService = snippetService;
    }

    @GetMapping
    public ResponseEntity<List<Snippet>> getMySnippets(@AuthenticationPrincipal OAuth2User user) {
        String userId = user.getAttribute("id").toString();
        return ResponseEntity.ok(snippetService.findByUserId(userId));
    }

    @PostMapping
    public ResponseEntity<Snippet> create(
            @AuthenticationPrincipal OAuth2User user,
            @Valid @RequestBody Snippet snippet) {
        snippet.setUserId(user.getAttribute("id").toString());
        return ResponseEntity.ok(snippetService.save(snippet));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Snippet> update(
            @AuthenticationPrincipal OAuth2User user,
            @PathVariable Long id,
            @Valid @RequestBody Snippet snippet) {
        return snippetService.findById(id)
                .filter(s -> s.getUserId().equals(user.getAttribute("id").toString()))
                .map(existing -> {
                    existing.setTitle(snippet.getTitle());
                    existing.setCode(snippet.getCode());
                    existing.setLanguage(snippet.getLanguage());
                    return ResponseEntity.ok(snippetService.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal OAuth2User user,
            @PathVariable Long id) {
        return snippetService.findById(id)
                .filter(s -> s.getUserId().equals(user.getAttribute("id").toString()))
                .map(s -> {
                    snippetService.delete(id);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/share/{shareId}")
    public ResponseEntity<Snippet> getShared(@PathVariable String shareId) {
        return snippetService.findByShareId(shareId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
