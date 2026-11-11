package com.yunshu.zhixue.support.storage;

import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.SetBucketPolicyArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * MinIO 实现：首次使用时就绪桶并设置「公开读取」策略
 * （头像 / Logo 属公开资源，/files/** 直链访问，04 §2.10）。
 */
@Service
public class MinioStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioStorageService.class);

    private static final String PUBLIC_READ_POLICY = """
            {
              "Version": "2012-10-17",
              "Statement": [
                {
                  "Effect": "Allow",
                  "Principal": {"AWS": ["*"]},
                  "Action": ["s3:GetObject"],
                  "Resource": ["arn:aws:s3:::%s/*"]
                }
              ]
            }
            """;

    private final MinioClient minioClient;
    private final MinioProperties properties;
    private volatile boolean bucketReady;

    public MinioStorageService(MinioClient minioClient, MinioProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    @Override
    public String upload(InputStream inputStream, long size, String contentType, String objectName) {
        ensureBucket();
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectName)
                    .stream(inputStream, size, -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception ex) {
            log.error("对象上传失败：{}", objectName, ex);
            throw new BizException(ErrorCode.INTERNAL_ERROR, "文件上传失败，请稍后重试");
        }
        return publicUrl(objectName);
    }

    @Override
    public String publicUrl(String objectName) {
        return properties.getPublicUrl() + "/" + properties.getBucket() + "/" + objectName;
    }

    private void ensureBucket() {
        if (bucketReady) {
            return;
        }
        synchronized (this) {
            if (bucketReady) {
                return;
            }
            String bucket = properties.getBucket();
            try {
                boolean exists = minioClient.bucketExists(
                        BucketExistsArgs.builder().bucket(bucket).build());
                if (!exists) {
                    minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                    log.info("已创建对象存储桶：{}", bucket);
                }
                minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                        .bucket(bucket)
                        .config(PUBLIC_READ_POLICY.formatted(bucket))
                        .build());
                bucketReady = true;
            } catch (Exception ex) {
                log.error("对象存储初始化失败", ex);
                throw new BizException(ErrorCode.INTERNAL_ERROR, "对象存储暂不可用");
            }
        }
    }
}
