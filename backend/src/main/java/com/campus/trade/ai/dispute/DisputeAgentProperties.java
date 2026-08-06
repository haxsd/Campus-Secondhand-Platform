package com.campus.trade.ai.dispute;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 纠纷 Agent 配置。
 *
 * <p>默认关闭是安全边界：没有明确打开开关时，系统不会产生模型调用费用。</p>
 */
@ConfigurationProperties(prefix = "ai.dispute")
public class DisputeAgentProperties {
    private boolean enabled;
    private String model = "deepseek-v4-flash";
    private Duration requestTimeout = Duration.ofSeconds(35);
    private Duration staleAfter = Duration.ofMinutes(10);
    private Duration retryBackoff = Duration.ofSeconds(1);
    private int maxRetries = 1;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Duration getRequestTimeout() { return requestTimeout; }
    public void setRequestTimeout(Duration value) { this.requestTimeout = value; }
    public Duration getStaleAfter() { return staleAfter; }
    public void setStaleAfter(Duration value) { this.staleAfter = value; }
    public Duration getRetryBackoff() { return retryBackoff; }
    public void setRetryBackoff(Duration value) { this.retryBackoff = value; }
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int value) { this.maxRetries = value; }
}
