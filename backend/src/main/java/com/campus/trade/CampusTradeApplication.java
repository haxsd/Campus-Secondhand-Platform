package com.campus.trade;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 校园二手交易平台后端入口。
 *
 * <p>根包固定为 {@code com.campus.trade}，Spring 会自动扫描其下的配置类、
 * Controller、Service 和 Mapper，新增业务模块时不要放到根包之外。</p>
 */
@SpringBootApplication
public class CampusTradeApplication {

    /**
     * 启动 Spring Boot 应用。
     *
     * @param args 命令行启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(CampusTradeApplication.class, args);
    }
}
