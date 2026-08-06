package com.campus.trade.ai.dispute;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/** 纠纷 Agent 使用独立线程池，避免模型慢请求挤占商品审核任务。 */
@Configuration
public class DisputeAgentAsyncConfig {
    /** 创建纠纷辅助专用执行器。 */
    @Bean("disputeAiAssistExecutor")
    public Executor disputeAiAssistExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("dispute-ai-assist-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
