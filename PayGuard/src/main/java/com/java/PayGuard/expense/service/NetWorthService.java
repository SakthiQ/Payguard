package com.java.PayGuard.expense.service;

import com.java.PayGuard.expense.dto.NetWorthDto;
import com.java.PayGuard.expense.entity.AccountType;
import com.java.PayGuard.expense.entity.ExpenseEntry;
import com.java.PayGuard.expense.entity.SavingsGoal;
import com.java.PayGuard.expense.repository.ExpenseRepository;
import com.java.PayGuard.expense.repository.SavingsGoalRepository;
import com.java.PayGuard.user.entity.User;
import com.java.PayGuard.wallet.entity.Wallet;
import com.java.PayGuard.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NetWorthService {

    private final WalletRepository walletRepository;
    private final SavingsGoalRepository savingsGoalRepository;
    private final ExpenseRepository expenseRepository;

    @Transactional(readOnly = true)
    public NetWorthDto calculateNetWorth(User currentUser) {
        BigDecimal totalAssets = BigDecimal.ZERO;
        BigDecimal totalLiabilities = BigDecimal.ZERO;

        // Assets 1: PayGuard Wallet Balance
        Optional<Wallet> walletOpt = walletRepository.findByUserId(currentUser.getId());
        if (walletOpt.isPresent()) {
            totalAssets = totalAssets.add(walletOpt.get().getBalance());
        }

        // Assets 2: Savings Goals Accumulated Balance
        List<SavingsGoal> goals = savingsGoalRepository.findAllByUserOrderByCreatedAtDesc(currentUser);
        for (SavingsGoal goal : goals) {
            totalAssets = totalAssets.add(goal.getCurrentAmount());
        }

        // Liabilities: Expenses charged under CREDIT account type
        List<ExpenseEntry> creditExpenses = expenseRepository.filterExpenses(currentUser, null, AccountType.CREDIT, null, null, null, null);
        for (ExpenseEntry e : creditExpenses) {
            totalLiabilities = totalLiabilities.add(e.getAmount());
        }

        BigDecimal netWorth = totalAssets.subtract(totalLiabilities).setScale(4, RoundingMode.HALF_EVEN);

        return NetWorthDto.builder()
                .totalAssets(totalAssets.setScale(4, RoundingMode.HALF_EVEN))
                .totalLiabilities(totalLiabilities.setScale(4, RoundingMode.HALF_EVEN))
                .netWorth(netWorth)
                .currency("USD")
                .build();
    }
}
