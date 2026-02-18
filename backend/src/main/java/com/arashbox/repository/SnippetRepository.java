package com.arashbox.repository;

import com.arashbox.model.Snippet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SnippetRepository extends JpaRepository<Snippet, Long> {

    List<Snippet> findByUserIdOrderByUpdatedAtDesc(String userId);

    Optional<Snippet> findByShareId(String shareId);
}
