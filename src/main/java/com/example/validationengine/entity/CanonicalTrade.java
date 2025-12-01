package com.example.validationengine.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.BatchSize;

@Data
@Entity
@BatchSize(size = 100)
@Table(name = "canonical_trades", indexes = {
    @Index(name = "idx_transaction_id", columnList = "transaction_id"),
    @Index(name = "idx_trade_datetime", columnList = "trade_datetime"),
    @Index(name = "idx_client_account", columnList = "client_account_no")
},
uniqueConstraints = {
        @UniqueConstraint(columnNames = {"file_id", "order_id"})
    }
)
public class CanonicalTrade {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "file_id")
    private UUID fileId;

    @Column(name = "order_id")
    private Integer orderId;

    @Column(name = "status")
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "originator_type")
    private Integer originatorType;
    
    @Column(name = "firm_number")
    private Integer firmNumber;
    
    @Column(name = "fund_number")
    private Integer fundNumber;
    
    @Column(name = "transaction_type")
    private String transactionType;
    
    @Column(name = "transaction_id")
    private String transactionId;
    
    @Column(name = "trade_datetime")
    private LocalDateTime tradeDateTime;
    
    @Column(name = "dollar_amount")
    private BigDecimal dollarAmount;
    
    @Column(name = "client_account_no")
    private Integer clientAccountNo;
    
    @Column(name = "client_name")
    private String clientName;
    
    @Column(name = "ssn")
    private String ssn;
    
    @Column(name = "dob")
    private LocalDate dob;
    
    @Column(name = "share_quantity")
    private BigDecimal shareQuantity;
}