package com.java.PayGuard.expense.repository;

import com.java.PayGuard.expense.entity.ActivityInvoice;
import com.java.PayGuard.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityInvoiceRepository extends JpaRepository<ActivityInvoice, Long> {
    List<ActivityInvoice> findAllByUserOrderByCreatedAtDesc(User user);
}
