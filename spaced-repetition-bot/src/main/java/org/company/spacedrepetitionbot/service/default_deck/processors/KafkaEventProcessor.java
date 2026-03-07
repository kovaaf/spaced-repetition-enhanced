package org.company.spacedrepetitionbot.service.default_deck.processors;

import lombok.RequiredArgsConstructor;
import org.company.spacedrepetitionbot.service.default_deck.event.SyncEventDTO;
import org.company.spacedrepetitionbot.service.default_deck.processors.kafka.KafkaSyncEventProducer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.sync.mode", havingValue = "KAFKA")
@RequiredArgsConstructor
public class KafkaEventProcessor implements SyncEventProcessor {
    private final KafkaSyncEventProducer kafkaProducer;

    @Override
    public void processSyncEvent(SyncEventDTO event) {
        kafkaProducer.sendSyncEvent(event);
    }
}
