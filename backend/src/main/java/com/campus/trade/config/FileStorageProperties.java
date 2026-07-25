package com.campus.trade.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 本地文件存储配置，对应 application-dev.yml 的 {@code campus.file}。
 *
 * @param uploadDir 上传文件落盘目录；支持通过 CT_UPLOAD_DIR 环境变量覆盖
 */
@ConfigurationProperties(prefix = "campus.file")
public record FileStorageProperties(String uploadDir) {
}
