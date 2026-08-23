package com.java.PayGuard.expense.service;

import com.java.PayGuard.common.exception.ResourceNotFoundException;
import com.java.PayGuard.expense.dto.SubscriptionDto;
import com.java.PayGuard.expense.entity.SubscriptionTracker;
import com.java.PayGuard.expense.repository.SubscriptionTrackerRepository;
import com.java.PayGuard.notification.service.NotificationService;
import com.java.PayGuard.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final SubscriptionTrackerRepository subscriptionTrackerRepository;
    private final NotificationService notificationService;

    @Transactional
    public SubscriptionDto addSubscription(User currentUser, String serviceName, BigDecimal amount, String billingCycle, LocalDate nextRenewalDate) {
        SubscriptionTracker sub = SubscriptionTracker.builder()
                .user(currentUser)
                .serviceName(serviceName.trim())
                .amount(amount.setScale(4, RoundingMode.HALF_EVEN))
                .billingCycle(billingCycle != null ? billingCycle.toUpperCase() : "MONTHLY")
                .nextRenewalDate(nextRenewalDate)
                .status("ACTIVE")
                .build();

        SubscriptionTracker saved = subscriptionTrackerRepository.save(sub);
        log.info("Added subscription #{} '{}' for user {}", saved.getId(), saved.getServiceName(), currentUser.getEmail());
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<SubscriptionDto> getSubscriptions(User currentUser) {
        return subscriptionTrackerRepository.findAllByUserOrderByNextRenewalDateAsc(currentUser).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public SubscriptionDto requestCancellation(Long subscriptionId, User currentUser, String notes) {
        SubscriptionTracker sub = subscriptionTrackerRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found with ID: " + subscriptionId));

        if (!sub.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Unauthorized to cancel this subscription.");
        }

        sub.setStatus("CANCEL_REQUESTED");
        sub.setCancellationNotes(notes != null ? notes.trim() : "Requested 1-click subscription cancellation");
        SubscriptionTracker updated = subscriptionTrackerRepository.save(sub);

        notificationService.broadcast("SUBSCRIPTION_CANCEL_REQUESTED", Map.of(
                "subscriptionId", updated.getId(),
                "serviceName", updated.getServiceName(),
                "amount", updated.getAmount().toString()
        ));

        log.info("Cancellation requested for subscription #{} '{}'", updated.getId(), updated.getServiceName());
        return mapToDto(updated);
    }

    private SubscriptionDto mapToDto(SubscriptionTracker sub) {
        long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), sub.getNextRenewalDate());

        return SubscriptionDto.builder()
                .id(sub.getId())
                .serviceName(sub.getServiceName())
                .amount(sub.getAmount())
                .billingCycle(sub.getBillingCycle())
                .nextRenewalDate(sub.getNextRenewalDate())
                .daysUntilRenewal(daysUntil)
                .status(sub.getStatus())
                .cancellationNotes(sub.getCancellationNotes())
                .build();
    }
}
