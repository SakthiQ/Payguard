package com.java.PayGuard.expense.repository;

import com.java.PayGuard.expense.entity.SubscriptionTracker;
import com.java.PayGuard.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SubscriptionTrackerRepository extends JpaRepository<SubscriptionTracker, Long> {
    List<SubscriptionTracker> findAllByUserOrderByNextRenewalDateAsc(User user);
    List<SubscriptionTracker> findAllByNextRenewalDateBetweenAndStatus(LocalDate start, LocalDate end, String status);
}
