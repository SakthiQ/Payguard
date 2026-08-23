package com.java.PayGuard.transaction.repository;

import com.java.PayGuard.transaction.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionReference(String transactionReference);

    @Query("SELECT t FROM Transaction t WHERE t.senderWallet.id = :walletId OR t.receiverWallet.id = :walletId ORDER BY t.createdAt DESC")
    List<Transaction> findAllByWalletId(@Param("walletId") Long walletId);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.senderWallet.id = :senderWalletId AND t.createdAt >= :since")
    long countRecentTransfersBySender(@Param("senderWalletId") Long senderWalletId, @Param("since") LocalDateTime since);
}
