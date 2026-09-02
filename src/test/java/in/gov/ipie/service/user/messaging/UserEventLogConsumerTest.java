package in.gov.ipie.service.user.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.junit.jupiter.api.Test;

import in.gov.ipie.common.audit.model.AuditEvent;
import in.gov.ipie.common.audit.model.AuditEventType;
import in.gov.ipie.common.audit.outbox.OutboxAuditRecorder;
import in.gov.ipie.common.events.envelope.EventEnvelope;
import in.gov.ipie.common.events.idempotency.ProcessedEventStore;

class UserEventLogConsumerTest {

    private final ProcessedEventStore processedEventStore = mock(ProcessedEventStore.class);
    private final AuditTrailJpaRepository auditTrailRepository = mock(AuditTrailJpaRepository.class);
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final UserEventLogConsumer consumer =
            new UserEventLogConsumer(processedEventStore, auditTrailRepository, objectMapper);

    @Test
    void onEvent_marksANewEventAsProcessed() {
        EventEnvelope<String> event = EventEnvelope.create("USER_CREATED", 1, "test", null, null, "user-1");
        when(processedEventStore.isProcessed(event.eventId())).thenReturn(false);

        consumer.onEvent(event);

        verify(processedEventStore).isProcessed(event.eventId());
        verify(processedEventStore).markProcessed(event.eventId());
    }

    @Test
    void onEvent_skipsAnAlreadyProcessedEvent() {
        EventEnvelope<String> event = EventEnvelope.create("USER_CREATED", 1, "test", null, null, "user-1");
        when(processedEventStore.isProcessed(event.eventId())).thenReturn(true);

        consumer.onEvent(event);

        verify(processedEventStore).isProcessed(event.eventId());
        verifyNoMoreInteractions(processedEventStore);
    }

    @Test
    void onEvent_persistsAnAuditTrailRow_whenTheEventIsAnAuditEvent() {
        AuditEvent auditEvent = new AuditEvent(
                AuditEventType.BUSINESS, "USER_CREATED", "USER", "user-1", null, "actor-1", "127.0.0.1",
                "ipie-user-service", "created via admin", null, null, "corr-1", Instant.now());
        Map<String, Object> asMap = objectMapper.convertValue(auditEvent, Map.class);
        EventEnvelope<Map<String, Object>> event =
                EventEnvelope.create(OutboxAuditRecorder.AUDIT_EVENT_TYPE, 1, "ipie-user-service", "corr-1", null, asMap);
        when(processedEventStore.isProcessed(event.eventId())).thenReturn(false);

        consumer.onEvent(event);

        verify(auditTrailRepository).save(any(AuditTrailEntity.class));
    }

    @Test
    void onEvent_doesNotTouchAuditTrail_forAnOrdinaryDomainEvent() {
        EventEnvelope<String> event = EventEnvelope.create("USER_CREATED", 1, "test", null, null, "user-1");
        when(processedEventStore.isProcessed(event.eventId())).thenReturn(false);

        consumer.onEvent(event);

        verify(auditTrailRepository, never()).save(any());
    }
}
