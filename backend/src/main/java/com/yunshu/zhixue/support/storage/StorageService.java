package com.yunshu.zhixue.support.storage;

import java.io.InputStream;

/**
 * 对象存储抽象（02 §2.2）：上层只依赖本接口，不感知 MinIO / OSS 细节。
 *
 * <p>仅图片（Logo、默认头像、学员头像）入库；题目 Excel 与面试题 Markdown
 * 文件本身不入库，仅保留文档名与行号（02 §2.5）。</p>
 */
public interface StorageService {

    /**
     * 上传对象并返回可访问 URL。
     *
     * @param inputStream 数据流
     * @param size        字节数；未知时传 -1
     * @param contentType MIME 类型，如 image/png
     * @param objectName  对象名（由服务端生成，禁止使用用户可控路径）
     */
    String upload(InputStream inputStream, long size, String contentType, String objectName);

    /** 返回对象的公开访问地址。 */
    String publicUrl(String objectName);
}
