package com.yunshu.zhixue.modules.system.service;

import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.constant.OperationActions;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.modules.system.dto.ImageUploadResponse;
import com.yunshu.zhixue.support.storage.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;

/**
 * A-F01 图片上传（JPG/PNG，服务端校验格式与尺寸）。
 *
 * <p>Logo 建议 256×256、默认头像 200×200（04 §2.10），服务端统一限制单边不超过 256；
 * 对象名由服务端生成 UUID，禁止用户可控路径。</p>
 */
@Service
public class ImageUploadService {

    private static final long MAX_FILE_BYTES = 5L * 1024 * 1024;
    private static final int MAX_DIMENSION = 256;
    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final StorageService storageService;
    private final OperationLogRecorder operationLogRecorder;

    public ImageUploadService(StorageService storageService, OperationLogRecorder operationLogRecorder) {
        this.storageService = storageService;
        this.operationLogRecorder = operationLogRecorder;
    }

    public ImageUploadResponse upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.FILE_INVALID, "请选择要上传的图片");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new BizException(ErrorCode.FILE_INVALID, "图片大小不能超过 5MB");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new BizException(ErrorCode.FILE_INVALID, "图片读取失败");
        }

        ImageInfo image = inspect(bytes);
        if (image.width() > MAX_DIMENSION || image.height() > MAX_DIMENSION) {
            throw new BizException(ErrorCode.FILE_INVALID, "图片尺寸不能超过 " + MAX_DIMENSION + "×" + MAX_DIMENSION);
        }

        String objectName = "images/" + LocalDate.now(AppConstants.BUSINESS_ZONE).format(DATE_PATH)
                + "/" + UUID.randomUUID().toString().replace("-", "") + "." + image.extension();
        String url = storageService.upload(new ByteArrayInputStream(bytes), bytes.length,
                image.contentType(), objectName);
        operationLogRecorder.record(OperationActions.CREATE, "文件", "上传图片：" + objectName);
        return new ImageUploadResponse(url);
    }

    private ImageInfo inspect(byte[] bytes) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (input == null) {
                throw new BizException(ErrorCode.FILE_INVALID, "仅支持 JPG/PNG 图片");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new BizException(ErrorCode.FILE_INVALID, "仅支持 JPG/PNG 图片");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                return switch (format) {
                    case "png" -> new ImageInfo(width, height, "png", "image/png");
                    case "jpeg", "jpg" -> new ImageInfo(width, height, "jpg", "image/jpeg");
                    default -> throw new BizException(ErrorCode.FILE_INVALID, "仅支持 JPG/PNG 图片");
                };
            } finally {
                reader.dispose();
            }
        } catch (IOException ex) {
            throw new BizException(ErrorCode.FILE_INVALID, "图片解析失败");
        }
    }

    private record ImageInfo(int width, int height, String extension, String contentType) {
    }
}
