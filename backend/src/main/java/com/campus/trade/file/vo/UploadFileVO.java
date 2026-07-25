package com.campus.trade.file.vo;

/**
 * 单文件上传成功后的响应数据。
 *
 * @param url 后端静态资源访问路径，例如 /api/uploads/随机文件名.jpg
 */
public record UploadFileVO(String url) {
}
