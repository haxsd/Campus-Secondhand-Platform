package com.campus.trade.file.service;

import com.campus.trade.common.exception.BizException;
import com.campus.trade.config.FileStorageProperties;
import com.campus.trade.file.vo.UploadFileVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 本地文件存储单元测试。
 *
 * <p>使用 JUnit 自动管理的临时目录，不会在项目 uploads 目录留下测试文件。</p>
 */
class FileStorageServiceTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void shouldStorePngWithRandomServerFilename() throws Exception {
        FileStorageService storageService = storageService();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "user-provided-name.png",
                "image/png",
                pngHeader()
        );

        UploadFileVO uploaded = storageService.storeImage(file);

        assertThat(uploaded.url()).startsWith("/api/uploads/").endsWith(".png");
        // 返回 URL 去掉 /api/uploads/ 前缀后，应该能在受控上传目录找到对应文件。
        Path savedFile = temporaryDirectory.resolve(uploaded.url().substring("/api/uploads/".length()));
        assertThat(Files.exists(savedFile)).isTrue();
        assertThat(Files.readAllBytes(savedFile)).isEqualTo(pngHeader());
    }

    @Test
    void shouldRejectFakeImageWhenMagicHeaderIsInvalid() {
        FileStorageService storageService = storageService();
        MockMultipartFile fakeImage = new MockMultipartFile(
                "file",
                "fake.png",
                "image/png",
                "this is not a png file".getBytes()
        );

        assertThatThrownBy(() -> storageService.storeImage(fakeImage))
                .isInstanceOf(BizException.class)
                .hasMessage("只支持 jpg、png、webp 格式的图片");
    }

    @Test
    void shouldUseFileMagicInsteadOfUnreliableBrowserMimeMetadata() {
        FileStorageService storageService = storageService();
        MockMultipartFile mislabeledFile = new MockMultipartFile(
                "file",
                "photo.jpg",
                "image/jpeg",
                pngHeader()
        );

        // 浏览器可能把真实 PNG 错报成 image/jpeg；服务端应以 PNG 文件头为准并保存为 .png。
        UploadFileVO uploaded = storageService.storeImage(mislabeledFile);
        assertThat(uploaded.url()).endsWith(".png");
    }

    private FileStorageService storageService() {
        FileStorageService storageService = new FileStorageService(
                new FileStorageProperties(temporaryDirectory.toString())
        );
        storageService.initializeDirectory();
        return storageService;
    }

    private byte[] pngHeader() {
        // PNG 的标准 8 字节签名，后面补 4 字节使服务读取 12 字节时行为与真实图片一致。
        return new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D
        };
    }
}
