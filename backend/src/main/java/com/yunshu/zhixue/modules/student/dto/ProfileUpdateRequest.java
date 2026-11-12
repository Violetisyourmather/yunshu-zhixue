package com.yunshu.zhixue.modules.student.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * P-M02 修改资料入参。
 *
 * <p>字段均可选，未传表示保持原值；邮箱与头像传空字符串表示清空。
 * 用户名传入时必填且长度为 1~20 个字符，由 Service 做最终校验。</p>
 */
public record ProfileUpdateRequest(
        @Size(max = 20, message = "用户名长度为 1~20 个字符")
        String nickname,

        @Email(message = "邮箱格式不正确")
        @Size(max = 100, message = "邮箱长度不能超过 100 个字符")
        String email,

        @Min(value = 0, message = "性别值不合法")
        @Max(value = 2, message = "性别值不合法")
        Integer gender,

        Long preferredSubjectId,

        @Size(max = 500, message = "头像地址过长")
        String avatar) {
}
