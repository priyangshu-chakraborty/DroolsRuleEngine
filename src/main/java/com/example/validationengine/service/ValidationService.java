package com.example.validationengine.service;

import com.example.validationengine.dto.ValidationResult;
import com.example.validationengine.entity.Fund;
import com.example.validationengine.repository.CanonicalTradeRepository;
import com.example.validationengine.entity.CanonicalTrade;
import com.example.validationengine.entity.Client;
import com.example.validationengine.repository.FundRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;

import com.example.validationengine.repository.ClientRepository;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ValidationService {

    @Autowired
    private RuleEngineService ruleEngineService;
    
    @Autowired
    private OutboxService outboxService;

    @Autowired
    private CanonicalTradeRepository canonicalTradeRepository;

    @Autowired
	private ObjectMapper objectMapper;

    private final KieContainer kieContainer;
    private final String navCutoffTime;
    private final Double minimumInvestmentAmount;
    private final FundRepository fundRepository;
    private final ClientRepository clientRepository;

    public ValidationService(KieContainer kieContainer,
                             @Value("${validation.nav.cutoff:15:00}") String navCutoffTime,
                             @Value("${validation.minimum.amount:1000}") Double minimumInvestmentAmount,
                             FundRepository fundRepository,
                             ClientRepository clientRepository, CanonicalTradeRepository canonicalTradeRepository) {
        this.kieContainer = kieContainer;
        this.navCutoffTime = navCutoffTime;
        this.minimumInvestmentAmount = minimumInvestmentAmount;
        this.fundRepository = fundRepository;
        this.clientRepository = clientRepository;
        this.canonicalTradeRepository = canonicalTradeRepository;
    }



    public ValidationResult validate(CanonicalTrade data) {
    KieSession kieSession = null;
    try {
        kieSession = kieContainer.newKieSession();
        ValidationResult result = new ValidationResult();

        // Insert the incoming trade and result
        kieSession.insert(data);
        kieSession.insert(result);

        // Set required globals
        kieSession.setGlobal("ruleEngineService", ruleEngineService);
        kieSession.setGlobal("navCutoffTime", LocalTime.parse(navCutoffTime)); // replace with pre-parsed LocalTime if you have one
        kieSession.setGlobal("minimumInvestmentAmount", minimumInvestmentAmount);
        kieSession.setGlobal("requestId", UUID.randomUUID().toString());

        // --- Insert FundDTOs as facts ---
        List<Fund> funds = fundRepository.findAll();
        for (Fund f : funds) {
            com.example.validationengine.dto.FundDTO fundDto = new com.example.validationengine.dto.FundDTO();
            fundDto.setFundId(f.getFundId());
            fundDto.setSchemeCode(f.getSchemeCode());
            fundDto.setStatus(f.getStatus());
            // type is BigDecimal in DTO (you said you changed it)
            fundDto.setMinLimit(f.getMinLimit());
            fundDto.setMaxLimit(f.getMaxLimit());
            kieSession.insert(fundDto);
        }

        // --- Insert ClientDTOs as facts ---
        List<Client> clients = clientRepository.findAll();
        for (Client c : clients) {
            com.example.validationengine.dto.ClientDTO clientDto = new com.example.validationengine.dto.ClientDTO();
            clientDto.setClientId(c.getClientId());
            clientDto.setKycStatus(c.getKycStatus());
            clientDto.setPanNumber(c.getPanNumber());
            clientDto.setStatus(c.getStatus());
            clientDto.setType(c.getType());
            kieSession.insert(clientDto);
        }

        // Fire rules
        kieSession.fireAllRules();
        return result;
    } finally {
        if (kieSession != null) kieSession.dispose();
    }
    }


    @Transactional
    public void storeValidOrders(CanonicalTrade trade) {
    	/*
    	// Check if order already exists
        boolean tradeExists = canonicalTradeRepository.existsByFileIdAndOrderId(
                trade.getFileId(),
                trade.getOrderId()
        );

        if (tradeExists) {
            throw new RuntimeException(
                "Duplicate trade found for fileId=" + trade.getFileId() +
                ", orderId=" + trade.getOrderId()
            );
        }
        */

        // Save trade if duplicate not found (entity itself has composite key unique constraint)
        CanonicalTrade savedTrade = canonicalTradeRepository.save(trade);
        outboxService.createOutboxEntry(savedTrade);

    }
    
    // Batch processing to improve performance
    @Transactional
    public void storeValidOrdersBatch(List<CanonicalTrade> trades) {
        // Batch insert all valid trades
        List<CanonicalTrade> savedTrades = canonicalTradeRepository.saveAll(trades);

        // Create outbox entries IN BULK
        outboxService.createOutboxEntries(savedTrades);
    }


    // private void setFundAndClientGlobals(KieSession kieSession) {
    //     // load all funds
    //     List<Fund> funds = fundRepository.findAll();
    //     Map<Integer, Map<String, Object>> fundData = new HashMap<>(funds.size());
    //     for (Fund f : funds) {
    //         Map<String, Object> fm = new HashMap<>();
    //         fm.put("scheme_code", f.getSchemeCode());
    //         fm.put("status", f.getStatus());
    //         // store numeric values as Number so DRL can cast
    //         fm.put("max_limit", f.getMaxLimit() != null ? f.getMaxLimit() : null);
    //         fm.put("min_limit", f.getMinLimit() != null ? f.getMinLimit() : null);
    //         fundData.put(f.getFundId(), fm);
    //     }

    //     List<Client> clients = clientRepository.findAll();
    //     Map<Integer, Map<String, Object>> clientData = new HashMap<>(clients.size());
    //     for (Client c : clients) {
    //         Map<String, Object> cm = new HashMap<>();
    //         cm.put("kyc_status", c.getKycStatus());
    //         cm.put("pan_number", c.getPanNumber());
    //         cm.put("status", c.getStatus());
    //         cm.put("type", c.getType());
    //         clientData.put(c.getClientId(), cm);
    //     }

    //     kieSession.setGlobal("fundData", fundData);
    //     kieSession.setGlobal("clientData", clientData);
    // }
}
