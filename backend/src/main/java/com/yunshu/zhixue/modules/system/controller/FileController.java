package com.yunshu.zhixue.modules.system.controller;

import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.modules.system.dto.ImageUploadResponse;
import com.yunshu.zhixue.modules.system.service.ImageUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 图片上传（04 §5.13 A-F01）：登录即可，无独立权限点。
 */
@Tag(name = "文件", description = "A-F01 图片上传")
@RestController
@RequestMapping("/api/admin/v1/files")
public class FileController {

    private final ImageUploadService imageUploadService;

    public FileController(ImageUploadService imageUploadService) {
        this.imageUploadService = imageUploadService;
    }

    @Operation(summary = "A-F01 上传图片（JPG/PNG，≤256×256）")
    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ImageUploadResponse> uploadImage(@RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(imageUploadService.upload(file));
    }
}
