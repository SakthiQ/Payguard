package com.java.PayGuard.expense.repository;

import com.java.PayGuard.expense.entity.SavingsGoal;
import com.java.PayGuard.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {
    List<SavingsGoal> findAllByUserOrderByCreatedAtDesc(User user);
}
