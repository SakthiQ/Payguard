package com.java.PayGuard.fraud.service;

import com.java.PayGuard.fraud.entity.FraudFlag;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AiRiskExplanationService {

    public String generateRiskInsight(FraudFlag flag) {
        if (flag == null) return "No risk telemetry available.";

        String rule = flag.getTriggeredRuleCode();
        int score = flag.getRiskScore() != null ? flag.getRiskScore() : 50;
        BigDecimal amount = flag.getTransaction() != null ? flag.getTransaction().getAmount() : BigDecimal.ZERO;

        StringBuilder sb = new StringBuilder();

        if ("HIGH_AMOUNT".equalsIgnoreCase(rule)) {
            sb.append("🤖 AI Risk Assessment (Score: ").append(score).append("/100): ");
            sb.append("Transfer of $").append(amount).append(" triggers High Value Anomalous Outflow threshold. ");
            sb.append("Primary risk vectors: Account Takeover (ATO) or Unsolicited Wire Solicitations. ");
            sb.append("Recommended Analyst Action: Confirm identity with sender via out-of-band phone call before approval.");
        } else if ("HIGH_VELOCITY".equalsIgnoreCase(rule)) {
            sb.append("🤖 AI Risk Assessment (Score: ").append(score).append("/100): ");
            sb.append("High Transfer Velocity pattern detected within short 300-second window. ");
            sb.append("Primary risk vectors: Scripted bot attack, structuring/smurfing, or compromised API credentials. ");
            sb.append("Recommended Analyst Action: Verify transaction memo and check destination account creation date.");
        } else {
            sb.append("🤖 AI Risk Assessment (Score: ").append(score).append("/100): ");
            sb.append("Rule '").append(rule).append("' triggered. Review transaction details and user history.");
        }

        return sb.toString();
    }
}
