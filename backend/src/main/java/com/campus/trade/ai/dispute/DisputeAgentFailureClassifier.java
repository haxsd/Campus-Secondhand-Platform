package com.campus.trade.ai.dispute;

import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Locale;

/**
 * 将模型调用异常分成超时、可重试和不可重试三类。
 *
 * <p>429、5xx、连接失败和读超时属于暂时性故障，最多重试一次；
 * 输出解析错误不能重试网络请求，而应直接记录 INVALID_OUTPUT。</p>
 */
@Component
public class DisputeAgentFailureClassifier {

    /** 把一次模型调用异常转换为业务层可识别的异常类型。 */
    public RuntimeException classify(RuntimeException exception) {
        if (isTimeout(exception)) {
            return new DisputeAgentTimeoutException("模型调用超时", exception);
        }
        if (isRetryable(exception)) {
            return new DisputeAgentRetryableException("模型服务暂时不可用", exception);
        }
        return exception;
    }

    /** 判断连接和读取超时，供测试和编排服务复用。 */
    public boolean isTimeout(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SocketTimeoutException || current instanceof java.util.concurrent.TimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        String message = String.valueOf(throwable.getMessage()).toLowerCase(Locale.ROOT);
        return message.contains("timeout") || message.contains("timed out") || message.contains("超时");
    }

    /** 判断 HTTP 429、HTTP 5xx 和连接异常。 */
    public boolean isRetryable(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ResourceAccessException) {
                return true;
            }
            if (current instanceof HttpStatusCodeException statusException) {
                int status = statusException.getStatusCode().value();
                return status == 429 || status >= 500;
            }
            current = current.getCause();
        }
        String message = String.valueOf(throwable.getMessage()).toLowerCase(Locale.ROOT);
        return message.contains("429") || message.contains("connection reset")
                || message.contains("connection refused") || message.contains("5xx");
    }
}
