package com.campus.trade.ai.rule;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductReviewRuleParserTest {

    private static final Path RULE_FILE = Path.of(
            "src", "main", "resources", "ai-rules", "product-rules-2026-01.md"
    );

    private final ProductReviewRuleParser parser = new ProductReviewRuleParser();

    @Test
    void parsesAllProductRules() throws IOException {
        List<ProductReviewRule> rules = parser.parse(RULE_FILE);

        assertEquals(15, rules.size());
        assertEquals("PRODUCT-001", rules.get(0).ruleId());
        assertEquals("PRODUCT-015", rules.get(14).ruleId());
        rules.forEach(rule -> assertEquals("PRODUCT_RULE", rule.domain()));
        assertEquals(
                OffsetDateTime.parse("2026-01-01T00:00:00+08:00"),
                rules.get(0).effectiveAtTime()
        );
    }

    @Test
    void rejectsMissingRuleId() {
        assertMissingField("ruleId");
    }

    @Test
    void rejectsMissingVersion() {
        assertMissingField("version");
    }

    @Test
    void rejectsMissingEffectiveAt() {
        assertMissingField("effectiveAt");
    }

    @Test
    void rejectsMissingDomain() {
        assertMissingField("domain");
    }

    @Test
    void rejectsDuplicateRuleIdentity() {
        String duplicate = VALID_BLOCK + "\n" + VALID_BLOCK;

        assertThrows(IllegalArgumentException.class, () -> parser.parse(duplicate));
    }

    @Test
    void producesStableBodyHash() {
        ProductReviewRule first = parser.parse(VALID_BLOCK).get(0);
        ProductReviewRule second = parser.parse(VALID_BLOCK).get(0);

        assertEquals(first.bodySha256(), second.bodySha256());
        assertEquals(64, first.bodySha256().length());
        assertNotEquals("", first.bodySha256());
    }

    @Test
    void rejectsUnexpectedDomain() {
        String source = VALID_BLOCK.replace("PRODUCT_RULE", "ORDER_RULE");

        assertThrows(IllegalArgumentException.class, () -> parser.parse(source));
    }

    @Test
    void rejectsUnclosedFrontMatter() {
        String source = """
                ---
                ruleId: PRODUCT-TEST-001
                version: 2026-01
                effectiveAt: 2026-01-01T00:00:00+08:00
                domain: PRODUCT_RULE
                title: 测试规则
                """;

        assertThrows(IllegalArgumentException.class, () -> parser.parse(source));
    }

    @Test
    void rejectsFrontMatterLineWithoutColon() {
        String source = VALID_BLOCK.replace("title: 测试规则", "title 测试规则");

        assertThrows(IllegalArgumentException.class, () -> parser.parse(source));
    }

    @Test
    void rejectsDuplicateFrontMatterField() {
        String source = VALID_BLOCK.replace(
                "version: 2026-01",
                "version: 2026-01\nversion: 2026-02"
        );

        assertThrows(IllegalArgumentException.class, () -> parser.parse(source));
    }

    @Test
    void rejectsStandaloneSeparatorInsideRuleBody() {
        String source = VALID_BLOCK + "\n---\n正文中的分隔行\n";

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(source)
        );

        assertEquals(true, exception.getMessage().contains("正文不得包含单独的 `---` 分隔行"));
        assertEquals(true, exception.getMessage().contains("PRODUCT-TEST-001"));
    }

    @Test
    void rejectsInvalidEffectiveAt() {
        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(VALID_BLOCK.replace(
                        "2026-01-01T00:00:00+08:00", "2026-01-01"
                ))
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(VALID_BLOCK.replace(
                        "2026-01-01T00:00:00+08:00", "不是时间"
                ))
        );
    }

    @Test
    void rejectsEmptySource() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(""));
    }

    private void assertMissingField(String field) {
        String source = VALID_BLOCK.replaceFirst("(?m)^" + field + ":.*\\R", "");

        assertThrows(IllegalArgumentException.class, () -> parser.parse(source));
    }

    private static final String VALID_BLOCK = """
            ---
            ruleId: PRODUCT-TEST-001
            version: 2026-01
            effectiveAt: 2026-01-01T00:00:00+08:00
            domain: PRODUCT_RULE
            title: 测试规则
            ---
            规则正文。
            判定提示：需要人工确认。
            管理员处理建议：补充材料。
            """;
}
