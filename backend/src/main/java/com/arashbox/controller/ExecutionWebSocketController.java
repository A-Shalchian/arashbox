package com.arashbox.controller;

import com.arashbox.dto.OutputFrame;
import com.arashbox.dto.WsExecuteRequest;
import com.arashbox.service.CodeExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Controller
public class ExecutionWebSocketController {

    private static final Logger log = LoggerFactory.getLogger(ExecutionWebSocketController.class);

    private final CodeExecutionService codeExecutionService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final ConcurrentHashMap<String, Boolean> activeSessions = new ConcurrentHashMap<>();

    public ExecutionWebSocketController(CodeExecutionService codeExecutionService,
                                        SimpMessagingTemplate messagingTemplate) {
        this.codeExecutionService = codeExecutionService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/execute")
    public void execute(WsExecuteRequest request, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = request.getSessionId();
        String stompSessionId = headerAccessor.getSessionId();

        if (sessionId == null || sessionId.isBlank()) {
            log.warn("Received execute request with no sessionId");
            return;
        }

        // Prevent duplicate executions per STOMP session
        String activeKey = stompSessionId + ":" + sessionId;
        if (activeSessions.putIfAbsent(activeKey, Boolean.TRUE) != null) {
            messagingTemplate.convertAndSend(
                    "/topic/execution/" + sessionId + "/output",
                    OutputFrame.error("Execution already in progress")
            );
            return;
        }

        String destination = "/topic/execution/" + sessionId + "/output";

        executor.submit(() -> {
            try {
                codeExecutionService.executeStreaming(
                        request.getCode(),
                        request.getLanguage(),
                        request.getStdin(),
                        frame -> messagingTemplate.convertAndSend(destination, frame)
                );
            } catch (Exception e) {
                log.error("WebSocket execution failed for session {}", sessionId, e);
                messagingTemplate.convertAndSend(destination, OutputFrame.error("Internal error"));
            } finally {
                activeSessions.remove(activeKey);
            }
        });
    }
}
