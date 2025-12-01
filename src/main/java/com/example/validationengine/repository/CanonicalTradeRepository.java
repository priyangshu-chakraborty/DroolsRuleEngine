package com.example.validationengine.repository;


import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.validationengine.entity.CanonicalTrade;

@Repository
public interface CanonicalTradeRepository extends JpaRepository<CanonicalTrade, UUID> {
	boolean existsByFileIdAndOrderId(UUID fileId, Integer orderId);
    List<CanonicalTrade> findByFileIdInAndOrderIdIn(List<UUID> fileIds, List<Integer> orderIds);
	
}
