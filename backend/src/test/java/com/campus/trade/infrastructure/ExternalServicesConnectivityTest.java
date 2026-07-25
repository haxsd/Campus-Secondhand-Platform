package com.campus.trade.infrastructure;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MySQL 与 Redis 的真实连接测试。
 *
 * <p>它依赖本机外部服务，默认不会执行，避免其他开发者或 CI 没启动容器时误报。
 * 需要验证环境时使用：{@code mvn -DrunExternalTests=true test}。</p>
 */
@Tag("integration")
@SpringBootTest
@EnabledIfSystemProperty(named = "runExternalTests", matches = "true")
class ExternalServicesConnectivityTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private RedissonClient redissonClient;

    @Test
    void shouldConnectToMySqlAndRedis() throws Exception {
        // SELECT 1 不修改业务数据，只验证连接、账号和 JDBC 参数是否正确。
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT 1")) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getInt(1)).isEqualTo(1);
        }

        // count() 会触发一次真实 Redis 请求，但不会创建或删除任何键。
        assertThat(redissonClient.getKeys().count()).isGreaterThanOrEqualTo(0L);
    }
}
