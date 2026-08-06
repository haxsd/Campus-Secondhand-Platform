package com.campus.trade.ai.rule;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ProductReviewRuleParser {

    public static final String PRODUCT_RULE_DOMAIN = "PRODUCT_RULE";

    private static final List<String> REQUIRED_FIELDS = List.of(
            "ruleId", "version", "effectiveAt", "domain", "title"
    );

    private final String expectedDomain;

    public ProductReviewRuleParser() {
        this(PRODUCT_RULE_DOMAIN);
    }

    public ProductReviewRuleParser(String expectedDomain) {
        if (expectedDomain == null || expectedDomain.isBlank()) {
            throw new IllegalArgumentException("expectedDomain must not be blank");
        }
        this.expectedDomain = expectedDomain;
    }

    public List<ProductReviewRule> parse(Path source) throws IOException {
        return parse(Files.readString(source, StandardCharsets.UTF_8));
    }

    public List<ProductReviewRule> parse(String source) {
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("rule source must not be blank");
        }

        String normalizedSource = source.replace("\r\n", "\n").replace('\r', '\n');
        List<String> lines = List.of(normalizedSource.split("\n", -1));
        List<ProductReviewRule> rules = new ArrayList<>();
        Set<String> identities = new HashSet<>();
        int separator = findSeparator(lines, 0);

        while (separator >= 0) {
            int metadataStart = separator + 1;
            int metadataEnd = findSeparator(lines, metadataStart);
            if (metadataEnd < 0) {
                if (!rules.isEmpty()) {
                    throw new IllegalArgumentException(
                            "规则块 " + rules.get(rules.size() - 1).ruleId()
                                    + " 第 " + (metadataStart + 1)
                                    + " 行附近的规则块格式不合法；正文不得包含单独的 `---` 分隔行"
                    );
                }
                throw new IllegalArgumentException(
                        "第 " + (separator + 1) + " 行附近的规则块 front matter 未闭合"
                );
            }

            Map<String, String> metadata;
            try {
                metadata = parseMetadata(lines, metadataStart, metadataEnd);
            } catch (IllegalArgumentException exception) {
                if (!rules.isEmpty()) {
                    throw new IllegalArgumentException(
                            "规则块 " + rules.get(rules.size() - 1).ruleId()
                                    + " 第 " + (metadataStart + 1)
                                    + " 行附近的规则块格式不合法；正文不得包含单独的 `---` 分隔行",
                            exception
                    );
                }
                throw exception;
            }
            int bodyStart = metadataEnd + 1;
            int nextSeparator = findSeparator(lines, bodyStart);
            int bodyEnd = nextSeparator >= 0 ? nextSeparator : lines.size();
            String body = joinLines(lines, bodyStart, bodyEnd).trim();
            rules.add(buildRule(metadata, body, identities));

            if (nextSeparator < 0) {
                break;
            }
            separator = nextSeparator;
        }

        if (rules.isEmpty()) {
            throw new IllegalArgumentException("no rule blocks found");
        }
        return List.copyOf(rules);
    }

    private ProductReviewRule buildRule(
            Map<String, String> metadata,
            String body,
            Set<String> identities
    ) {
        for (String field : REQUIRED_FIELDS) {
            if (!metadata.containsKey(field) || metadata.get(field).isBlank()) {
                throw new IllegalArgumentException("missing required field: " + field);
            }
        }
        if (!expectedDomain.equals(metadata.get("domain"))) {
            throw new IllegalArgumentException(
                    "domain mismatch: expected " + expectedDomain + ", got " + metadata.get("domain")
            );
        }
        OffsetDateTime effectiveAtTime;
        try {
            effectiveAtTime = OffsetDateTime.parse(metadata.get("effectiveAt"));
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                    "effectiveAt 格式非法: " + metadata.get("effectiveAt")
                            + "，必须是带时区偏移的 ISO-8601 时间",
                    exception
            );
        }
        if (body.isBlank()) {
            throw new IllegalArgumentException("rule body must not be blank: " + metadata.get("ruleId"));
        }

        String identity = metadata.get("domain") + "|" + metadata.get("ruleId")
                + "|" + metadata.get("version");
        if (!identities.add(identity)) {
            throw new IllegalArgumentException("duplicate rule identity: " + identity);
        }

        return new ProductReviewRule(
                metadata.get("ruleId"),
                metadata.get("version"),
                metadata.get("effectiveAt"),
                effectiveAtTime,
                metadata.get("domain"),
                metadata.get("title"),
                body,
                sha256(body)
        );
    }

    private static Map<String, String> parseMetadata(
            List<String> lines,
            int start,
            int end
    ) {
        Map<String, String> metadata = new HashMap<>();
        for (int i = start; i < end; i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }
            int colon = line.indexOf(':');
            if (colon <= 0) {
                throw new IllegalArgumentException(
                        "第 " + (i + 1) + " 行 front matter 缺少冒号: " + line
                );
            }
            String key = line.substring(0, colon).trim();
            String value = line.substring(colon + 1).trim();
            if (metadata.put(key, value) != null) {
                throw new IllegalArgumentException(
                        "第 " + (i + 1) + " 行 front matter 字段重复: " + key
                );
            }
        }
        return metadata;
    }

    private static int findSeparator(List<String> lines, int start) {
        for (int i = start; i < lines.size(); i++) {
            if ("---".equals(lines.get(i).trim())) {
                return i;
            }
        }
        return -1;
    }

    private static String joinLines(List<String> lines, int start, int end) {
        if (start >= end) {
            return "";
        }
        return String.join("\n", lines.subList(start, end));
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                hex.append(String.format("%02x", item));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
