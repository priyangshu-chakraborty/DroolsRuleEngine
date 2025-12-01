package com.example.validationengine.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.validationengine.entity.CanonicalTrade;
import com.example.validationengine.entity.OutboxEntity;
import com.example.validationengine.repository.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;

@Service
public class OutboxService {

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // Batch processed entries saved to outbox
    @Transactional
    public void createOutboxEntries(List<CanonicalTrade> savedTrades) {

        List<OutboxEntity> outboxList = new ArrayList<>();

        for (CanonicalTrade saved : savedTrades) {
            try {
                String payloadJson = objectMapper.writeValueAsString(saved);

                OutboxEntity outbox = new OutboxEntity();
                outbox.setAggregateId(saved.getId());
                outbox.setPayload(payloadJson);
                outbox.setStatus("ARRIVED");
                outbox.setCreatedAt(LocalDateTime.now());
                outbox.setRetryCount(0);

                outboxList.add(outbox);

            } catch (Exception e) {
                throw new RuntimeException("JSON conversion failed", e);
            }
        }

        outboxRepository.saveAll(outboxList);
    }
    
    // Saving single entry in outbox
    @Transactional
    public void createOutboxEntry(CanonicalTrade trade) {
    	// Convert saved trade to JSON for outbox payload
        final String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(trade);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert trade to JSON", e);
        }

        // Store in outbox
        OutboxEntity outbox = new OutboxEntity();

        outbox.setAggregateId(trade.getId());
        outbox.setPayload(payloadJson);
        outbox.setStatus("ARRIVED");
        outbox.setCreatedAt(LocalDateTime.now());
        outbox.setRetryCount(0);
        outbox.setLastAttemptAt(null);

        outboxRepository.save(outbox);
    }
}
