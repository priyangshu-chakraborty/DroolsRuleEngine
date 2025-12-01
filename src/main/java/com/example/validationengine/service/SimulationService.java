package com.example.validationengine.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.validationengine.dto.ValidationResult;
import com.example.validationengine.entity.CanonicalTrade;

@Service
public class SimulationService {

    @Autowired
    private ValidationService validationService;

    public void generateTrades(int count) {
    	List<CanonicalTrade> tradeList = new ArrayList<>();
    	
        for (int i = 0; i < count; i++) {

            CanonicalTrade trade = new CanonicalTrade();

            trade.setFileId(UUID.randomUUID());
            trade.setOrderId(i + 1);
            trade.setStatus("NEW");
            trade.setCreatedAt(LocalDateTime.now());

            trade.setOriginatorType(1);
            trade.setFirmNumber(100);
            trade.setFundNumber(1); 

            // Transaction type: B or S or E
            trade.setTransactionType(i % 2 == 0 ? "B" : "S");

            trade.setTransactionId("TXN-" + i);

            trade.setTradeDateTime(LocalDateTime.now());

            if (trade.getTransactionType().equals("B")) {
                trade.setDollarAmount(BigDecimal.valueOf(1000 + i));
                trade.setShareQuantity(null);
            }

            if (trade.getTransactionType().equals("S")) {
                trade.setShareQuantity(BigDecimal.valueOf(10));
                trade.setDollarAmount(null);
            }

            trade.setClientAccountNo(1);
            trade.setClientName("Simulated User");
            trade.setSsn("123-45-6789");
            trade.setDob(LocalDate.of(1990, 1, 1));

            ValidationResult result = validationService.validate(trade);

            if (result.isValid()) {
                trade.setStatus("VALID");
                tradeList.add(trade);
                //validationService.storeValidOrders(trade);
            } else {
                trade.setStatus("INVALID");
            }
        }
        
        validationService.storeValidOrdersBatch(tradeList);
    }
}

