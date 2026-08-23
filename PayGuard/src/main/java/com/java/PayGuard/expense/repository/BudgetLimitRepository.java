package com.java.PayGuard.expense.repository;

import com.java.PayGuard.expense.entity.BudgetLimit;
import com.java.PayGuard.expense.entity.ExpenseCategory;
import com.java.PayGuard.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetLimitRepository extends JpaRepository<BudgetLimit, Long> {
    List<BudgetLimit> findAllByUser(User user);
    Optional<BudgetLimit> findByUserAndCategory(User user, ExpenseCategory category);
}
