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
 * 学员端文件上传（P-M05 头像）。
 *
 * <p>接口需学员登录；上传返回站内 / MinIO 公开直链，头像保存由 P-M02 完成。</p>
 */
@Tag(name = "学员文件", description = "P-M05 上传头像")
@RestController
@RequestMapping("/api/portal/v1/files")
public class PortalFileController {

    private final ImageUploadService imageUploadService;

    public PortalFileController(ImageUploadService imageUploadService) {
        this.imageUploadService = imageUploadService;
    }

    @Operation(summary = "P-M05 上传头像（JPG/PNG，尺寸不限）")
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ImageUploadResponse> uploadAvatar(@RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(imageUploadService.uploadAvatar(file));
    }
}
