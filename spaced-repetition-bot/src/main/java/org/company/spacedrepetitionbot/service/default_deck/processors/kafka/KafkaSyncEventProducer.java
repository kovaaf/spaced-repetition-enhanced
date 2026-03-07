package org.company.spacedrepetitionbot.service.default_deck.processors.kafka;

import lombok.RequiredArgsConstructor;
import org.company.spacedrepetitionbot.service.default_deck.event.SyncEventDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@ConditionalOnProperty(name = "app.sync.mode", havingValue = "KAFKA")
@Service
@RequiredArgsConstructor
public class KafkaSyncEventProducer {
    private final KafkaTemplate<String, SyncEventDTO> kafkaTemplate;
    @Value("${spring.kafka.topic.sync.events}")
    private String syncTopic;

    public void sendSyncEvent(SyncEventDTO event) {
        String key = event.getDeckId() != null ? event.getDeckId().toString() : "global-sync";
        kafkaTemplate.send(syncTopic, key, event);
    }
}
