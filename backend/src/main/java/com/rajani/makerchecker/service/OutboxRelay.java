package com.rajani.makerchecker.service;

import com.rajani.makerchecker.domain.OutboxEvent;
import com.rajani.makerchecker.repo.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Delivers outbox rows to the caller's webhook. At-least-once — consumers dedupe on
 * X-Event-Id. Swapping to Kafka means replacing the RestClient call below and nothing else.
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxEventRepository outbox;
    private final RestClient restClient = RestClient.create();

    public OutboxRelay(OutboxEventRepository outbox) {
        this.outbox = outbox;
    }

    @Scheduled(fixedDelay = 15_000)
    public void relay() {
        List<OutboxEvent> pending = outbox.findByDeliveredFalseOrderByCreatedAtAsc();
        for (OutboxEvent event : pending) {
            deliver(event);
        }
    }

    private void deliver(OutboxEvent event) {
        if (event.getCallbackUrl() == null || event.getCallbackUrl().isBlank()) {
            // No callback configured for this request: mark delivered so it stops being retried.
            event.setDelivered(true);
            event.setDeliveredAt(Instant.now());
            outbox.save(event);
            return;
        }
        try {
            String eventId = event.getId() == null ? UUID.randomUUID().toString() : event.getId();
            restClient.post()
                    .uri(event.getCallbackUrl())
                    .header("X-Event-Id", eventId)
                    .header("X-Event-Type", event.getEventType())
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(event.getPayload())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        throw new IllegalStateException("Callback returned " + resp.getStatusCode());
                    })
                    .toBodilessEntity();
            event.setDelivered(true);
            event.setDeliveredAt(Instant.now());
            outbox.save(event);
        } catch (Exception e) {
            event.setAttempts(event.getAttempts() + 1);
            event.setLastError(e.getMessage());
            outbox.save(event);
            log.warn("Outbox delivery failed for event {} (attempt {}): {}",
                    event.getId(), event.getAttempts(), e.getMessage());
        }
    }
}
