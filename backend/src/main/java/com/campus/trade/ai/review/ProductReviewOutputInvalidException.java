package com.campus.trade.ai.review;

public class ProductReviewOutputInvalidException extends RuntimeException {
    public ProductReviewOutputInvalidException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProductReviewOutputInvalidException(String message) {
        super(message);
    }
}
