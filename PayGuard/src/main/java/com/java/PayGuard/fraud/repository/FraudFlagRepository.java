package com.java.PayGuard.fraud.repository;

import com.java.PayGuard.fraud.entity.FlagStatus;
import com.java.PayGuard.fraud.entity.FraudFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FraudFlagRepository extends JpaRepository<FraudFlag, Long> {
    List<FraudFlag> findAllByStatus(FlagStatus status);
    Optional<FraudFlag> findByTransactionId(Long transactionId);
}
