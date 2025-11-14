package com.kaustubh.transactions.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.read-auth")
public class ReadAuthProperties {

    private String apiKeys;
    private String rateLimits;
    private String apiKeyMerchants;
    private int rateWindowSeconds = 60;
    private int defaultLimitPerWindow = 60;
    private int ownershipTtlHours = 24;
    private String redisConfigKey = "read-auth:config";

    public String getApiKeys() {
        return apiKeys;
    }

    public void setApiKeys(String apiKeys) {
        this.apiKeys = apiKeys;
    }

    public String getRateLimits() {
        return rateLimits;
    }

    public void setRateLimits(String rateLimits) {
        this.rateLimits = rateLimits;
    }

    public String getApiKeyMerchants() {
        return apiKeyMerchants;
    }

    public void setApiKeyMerchants(String apiKeyMerchants) {
        this.apiKeyMerchants = apiKeyMerchants;
    }

    public int getRateWindowSeconds() {
        return rateWindowSeconds;
    }

    public void setRateWindowSeconds(int rateWindowSeconds) {
        this.rateWindowSeconds = rateWindowSeconds;
    }

    public int getDefaultLimitPerWindow() {
        return defaultLimitPerWindow;
    }

    public void setDefaultLimitPerWindow(int defaultLimitPerWindow) {
        this.defaultLimitPerWindow = defaultLimitPerWindow;
    }

    public int getOwnershipTtlHours() {
        return ownershipTtlHours;
    }

    public void setOwnershipTtlHours(int ownershipTtlHours) {
        this.ownershipTtlHours = ownershipTtlHours;
    }

    public String getRedisConfigKey() {
        return redisConfigKey;
    }

    public void setRedisConfigKey(String redisConfigKey) {
        this.redisConfigKey = redisConfigKey;
    }
}
