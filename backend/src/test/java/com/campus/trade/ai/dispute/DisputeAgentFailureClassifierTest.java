package com.campus.trade.ai.dispute;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class DisputeAgentFailureClassifierTest {
    private final DisputeAgentFailureClassifier classifier = new DisputeAgentFailureClassifier();

    @Test
    void serverErrorAndConnectionFailureAreRetryable() {
        assertThat(classifier.isRetryable(HttpServerErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR, "error", null, null, null))).isTrue();
        assertThat(classifier.isRetryable(new ResourceAccessException("connection refused"))).isTrue();
    }

    @Test
    void socketTimeoutIsTimeoutNotOutputInvalid() {
        RuntimeException classified = classifier.classify(new ResourceAccessException("read timeout", new SocketTimeoutException("timeout")));
        assertThat(classified).isInstanceOf(DisputeAgentTimeoutException.class);
    }
}
