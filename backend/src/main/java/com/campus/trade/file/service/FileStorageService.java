package com.campus.trade.file.service;

import com.campus.trade.common.exception.BizException;
import com.campus.trade.common.exception.ErrorCode;
import com.campus.trade.config.FileStorageProperties;
import com.campus.trade.file.vo.UploadFileVO;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * 本地图片文件存储服务。
 *
 * <p>第一版将图片保存到本机目录，并由 WebMvcConfig 以 /uploads/** 对外静态访问。
 * 业务模块只保存返回的 URL，不感知真实磁盘路径；以后切换 OSS 时可以保持 Controller 和商品表字段不变，
 * 只替换本服务的存储实现。</p>
 */
@Service
@EnableConfigurationProperties(FileStorageProperties.class)
public class FileStorageService {

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    // 后端配置了 server.servlet.context-path=/api，前端开发服务器也只代理 /api，
    // 因此必须把 /api 一并返回，浏览器图片请求才能正确到达后端静态资源处理器。
    private static final String PUBLIC_URL_PREFIX = "/api/uploads/";

    private final Path uploadDirectory;

    public FileStorageService(FileStorageProperties properties) {
        // 转为绝对规范路径，后续拼接随机文件名时可以额外检查路径没有逃出上传目录。
        this.uploadDirectory = Path.of(properties.uploadDir()).toAbsolutePath().normalize();
    }

    /**
     * 应用启动时确保上传目录存在。目录已存在时 createDirectories 不会报错。
     */
    @PostConstruct
    public void initializeDirectory() {
        try {
            Files.createDirectories(uploadDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException("无法创建上传目录: " + uploadDirectory, exception);
        }
    }

    /**
     * 校验并保存一张商品图片。
     *
     * <p>以文件头魔数识别真实格式。浏览器上报的 MIME 类型只是客户端元数据，
     * 在 Windows、聊天软件或图片转换工具之间可能不一致，不能据此误拒绝真实图片。</p>
     */
    public UploadFileVO storeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "请选择要上传的图片");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BizException(ErrorCode.BAD_REQUEST, "单张图片不能超过 5MB");
        }

        // 文件头由服务端读取，不能靠“把文件名改成 .png”伪造；它是最终格式依据。
        ImageType imageType = detectImageType(file);

        // 完全忽略原文件名，使用 UUID 生成服务器文件名，避免路径穿越和重名覆盖。
        String savedFilename = UUID.randomUUID() + imageType.extension();
        Path target = uploadDirectory.resolve(savedFilename).normalize();
        if (!target.startsWith(uploadDirectory)) {
            // 理论上随机文件名不会触发；保留检查是为了防止未来改动引入路径穿越漏洞。
            throw new BizException(ErrorCode.BAD_REQUEST, "文件名不合法");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "图片保存失败，请稍后重试");
        }

        return new UploadFileVO(PUBLIC_URL_PREFIX + savedFilename);
    }

    /**
     * 向 WebMvcConfig 提供真实文件目录，用于注册静态资源映射。
     */
    public Path getUploadDirectory() {
        return uploadDirectory;
    }

    private ImageType detectImageType(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            // PNG 需要 8 字节，WebP 需要检查 RIFF 与第 8~11 字节的 WEBP，因此读取 12 字节。
            byte[] header = inputStream.readNBytes(12);
            if (isJpeg(header)) {
                return ImageType.JPEG;
            }
            if (isPng(header)) {
                return ImageType.PNG;
            }
            if (isWebp(header)) {
                return ImageType.WEBP;
            }
            throw new BizException(ErrorCode.BAD_REQUEST, "只支持 jpg、png、webp 格式的图片");
        } catch (IOException exception) {
            throw new BizException(ErrorCode.BAD_REQUEST, "无法读取上传文件");
        }
    }

    private boolean isJpeg(byte[] header) {
        return header.length >= 3
                && (header[0] & 0xFF) == 0xFF
                && (header[1] & 0xFF) == 0xD8
                && (header[2] & 0xFF) == 0xFF;
    }

    private boolean isPng(byte[] header) {
        return header.length >= 8
                && (header[0] & 0xFF) == 0x89
                && header[1] == 0x50
                && header[2] == 0x4E
                && header[3] == 0x47
                && header[4] == 0x0D
                && header[5] == 0x0A
                && header[6] == 0x1A
                && header[7] == 0x0A;
    }

    private boolean isWebp(byte[] header) {
        return header.length >= 12
                && header[0] == 'R'
                && header[1] == 'I'
                && header[2] == 'F'
                && header[3] == 'F'
                && header[8] == 'W'
                && header[9] == 'E'
                && header[10] == 'B'
                && header[11] == 'P';
    }

    /**
     * 文件头识别后的可信图片类型。扩展名由此类型生成，不使用用户提供的扩展名。
     */
    private enum ImageType {
        JPEG(".jpg", "image/jpeg"),
        PNG(".png", "image/png"),
        WEBP(".webp", "image/webp");

        private final String extension;
        private final String mimeType;

        ImageType(String extension, String mimeType) {
            this.extension = extension;
            this.mimeType = mimeType;
        }

        String extension() {
            return extension;
        }

        String mimeType() {
            return mimeType;
        }
    }
}
