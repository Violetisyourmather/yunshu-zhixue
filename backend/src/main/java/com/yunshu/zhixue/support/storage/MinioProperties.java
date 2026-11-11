package com.yunshu.zhixue.support.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 对象存储配置（S3 兼容，D8）。
 *
 * <p>代码仅依赖 S3 兼容接口语义，替换为云 OSS / COS 时改配置即可。</p>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.minio")
public class MinioProperties {

    private String endpoint = "http://127.0.0.1:9000";
    private String accessKey;
    private String secretKey;
    private String bucket = "yszx-media";
    /** 对外可访问的基础地址（部署时由 Nginx /files 或 MinIO 域名提供）。 */
    private String publicUrl = "http://127.0.0.1:9000";
}
