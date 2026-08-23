package com.java.PayGuard.notification.controller;

import com.java.PayGuard.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Real-time Notifications", description = "Server-Sent Events (SSE) live notification stream")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping(value = "/stream", produces = "text/event-stream")
    @Operation(summary = "Subscribe to live SSE events", description = "Establishes a persistent Server-Sent Events connection for real-time risk alerts and transaction updates.")
    public SseEmitter subscribe() {
        return notificationService.subscribe();
    }
}
