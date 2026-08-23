package com.java.PayGuard.expense.repository;

import com.java.PayGuard.expense.entity.AccountType;
import com.java.PayGuard.expense.entity.ExpenseCategory;
import com.java.PayGuard.expense.entity.ExpenseEntry;
import com.java.PayGuard.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<ExpenseEntry, Long> {

    List<ExpenseEntry> findAllByUserOrderByCreatedAtDesc(User user);

    List<ExpenseEntry> findAllByUserAndTaxDeductibleTrueOrderByCreatedAtDesc(User user);

    @Query("SELECT e FROM ExpenseEntry e WHERE e.user = :user " +
           "AND (:category IS NULL OR e.category = :category) " +
           "AND (:accountType IS NULL OR e.accountType = :accountType) " +
           "AND (:taxDeductible IS NULL OR e.taxDeductible = :taxDeductible) " +
           "AND (:query IS NULL OR LOWER(e.merchantName) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(e.description) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(e.tags) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND (:startDate IS NULL OR e.createdAt >= :startDate) " +
           "AND (:endDate IS NULL OR e.createdAt <= :endDate) " +
           "ORDER BY e.createdAt DESC")
    List<ExpenseEntry> filterExpenses(@Param("user") User user,
                                      @Param("category") ExpenseCategory category,
                                      @Param("accountType") AccountType accountType,
                                      @Param("taxDeductible") Boolean taxDeductible,
                                      @Param("query") String query,
                                      @Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);
}
