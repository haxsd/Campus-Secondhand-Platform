package com.campus.trade.ai.review;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "ai.review")
public class ProductReviewProperties {
    private boolean enabled;
    private String model = "deepseek-v4-flash";
    private String baseUrl = "https://api.deepseek.com";
    private Duration requestTimeout = Duration.ofSeconds(35);
    private Duration staleAfter = Duration.ofMinutes(10);
    private Duration retryBackoff = Duration.ofSeconds(1);
    private int maxRetries = 1;
    private String ruleDomain = "PRODUCT_RULE";
    private double autoRejectMinConfidence = 0.8;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public Duration getRequestTimeout() { return requestTimeout; }
    public void setRequestTimeout(Duration value) { this.requestTimeout = value; }
    public Duration getStaleAfter() { return staleAfter; }
    public void setStaleAfter(Duration value) { this.staleAfter = value; }
    public Duration getRetryBackoff() { return retryBackoff; }
    public void setRetryBackoff(Duration value) { this.retryBackoff = value; }
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int value) { this.maxRetries = value; }
    public String getRuleDomain() { return ruleDomain; }
    public void setRuleDomain(String value) { this.ruleDomain = value; }
    public double getAutoRejectMinConfidence() { return autoRejectMinConfidence; }
    public void setAutoRejectMinConfidence(double value) { this.autoRejectMinConfidence = value; }
}
