package com.campus.trade.file.controller;

import com.campus.trade.common.response.Result;
import com.campus.trade.file.service.FileStorageService;
import com.campus.trade.file.vo.UploadFileVO;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传接口。
 *
 * <p>该路径没有加入公开白名单，因此必须携带有效 JWT。当前版本只实现图片上传，
 * 后续纠纷证据和商品图片都可以复用同一接口。</p>
 */
@RestController
@RequestMapping("/files")
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    /**
     * 接收 multipart/form-data 中字段名为 file 的一张图片。
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<UploadFileVO> upload(@RequestParam("file") MultipartFile file) {
        return Result.ok(fileStorageService.storeImage(file));
    }
}
