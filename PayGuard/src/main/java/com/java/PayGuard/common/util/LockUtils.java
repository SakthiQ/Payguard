package com.java.PayGuard.common.util;

import lombok.Getter;

@Getter
public class LockUtils {

    public static class OrderedWalletIds {
        private final Long firstId;
        private final Long secondId;

        public OrderedWalletIds(Long firstId, Long secondId) {
            this.firstId = firstId;
            this.secondId = secondId;
        }

        public Long getFirstId() {
            return firstId;
        }

        public Long getSecondId() {
            return secondId;
        }
    }

    public static OrderedWalletIds sortWalletIds(Long walletIdA, Long walletIdB) {
        if (walletIdA.compareTo(walletIdB) < 0) {
            return new OrderedWalletIds(walletIdA, walletIdB);
        } else {
            return new OrderedWalletIds(walletIdB, walletIdA);
        }
    }
}
