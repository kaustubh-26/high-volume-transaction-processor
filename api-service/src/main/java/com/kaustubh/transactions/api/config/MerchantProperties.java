
package com.kaustubh.transactions.api.config;


import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.verify")
public class MerchantProperties {

    private Map<String, Merchant> merchants = new HashMap<>();
    private Duration allowedClockSkew = Duration.ofMinutes(2);
    private boolean nonceEnforced = false;
    private Duration nonceTtl = Duration.ofMinutes(2);

    public Map<String, Merchant> getMerchants() {
        return merchants;
    }

    public void setMerchants(Map<String, Merchant> merchants) {
        this.merchants = merchants;
    }

    public Duration getAllowedClockSkew() {
        return allowedClockSkew;
    }

    public void setAllowedClockSkew(Duration allowedClockSkew) {
        this.allowedClockSkew = allowedClockSkew;
    }

    public boolean isNonceEnforced() {
        return nonceEnforced;
    }

    public void setNonceEnforced(boolean nonceEnforced) {
        this.nonceEnforced = nonceEnforced;
    }

    public Duration getNonceTtl() {
        return nonceTtl;
    }

    public void setNonceTtl(Duration nonceTtl) {
        this.nonceTtl = nonceTtl;
    }

    public static class Merchant {
        private String saltKey;
        private String saltIndex;

        public String getSaltKey() {
            return saltKey;
        }

        public void setSaltKey(String saltKey) {
            this.saltKey = saltKey;
        }

        public String getSaltIndex() {
            return saltIndex;
        }

        public void setSaltIndex(String saltIndex) {
            this.saltIndex = saltIndex;
        }
    }
}
