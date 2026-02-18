package com.arashbox.service;

import com.arashbox.model.Snippet;
import com.arashbox.repository.SnippetRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SnippetService {

    private final SnippetRepository snippetRepository;

    public SnippetService(SnippetRepository snippetRepository) {
        this.snippetRepository = snippetRepository;
    }

    public Snippet save(Snippet snippet) {
        if (snippet.getShareId() == null) {
            snippet.setShareId(UUID.randomUUID().toString().substring(0, 8));
        }
        return snippetRepository.save(snippet);
    }

    public List<Snippet> findByUserId(String userId) {
        return snippetRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    public Optional<Snippet> findById(Long id) {
        return snippetRepository.findById(id);
    }

    public Optional<Snippet> findByShareId(String shareId) {
        return snippetRepository.findByShareId(shareId);
    }

    public void delete(Long id) {
        snippetRepository.deleteById(id);
    }
}
