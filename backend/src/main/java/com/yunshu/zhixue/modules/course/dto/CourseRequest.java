package com.yunshu.zhixue.modules.course.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * A-C03 / A-C04 新增或编辑课程。
 *
 * @param subjectId  主学科；{@code null} 表示「其他学科」
 * @param subjectIds 额外关联学科（不含主学科，可空）
 */
public record CourseRequest(
        @NotBlank(message = "课程名称不能为空")
        @Size(max = 100, message = "课程名称不能超过 100 个字符")
        String name,

        @Size(max = 500, message = "封面 URL 不能超过 500 个字符")
        String coverUrl,

        @Size(max = 500, message = "课程简介不能超过 500 个字符")
        String intro,

        @NotNull(message = "请输入课程时长")
        @Min(value = 1, message = "课程时长必须大于 0 分钟")
        @Max(value = 100000, message = "课程时长不能超过 100000 分钟")
        Integer durationMinutes,

        @NotNull(message = "请选择课程来源")
        @Min(value = 1, message = "课程来源不合法")
        @Max(value = 2, message = "课程来源不合法")
        Integer source,

        @Size(max = 500, message = "课程链接不能超过 500 个字符")
        String link,

        @Size(max = 20, message = "BV 号不能超过 20 个字符")
        String bvid,

        @NotNull(message = "请选择课程状态")
        @Min(value = 1, message = "课程状态不合法")
        @Max(value = 2, message = "课程状态不合法")
        Integer status,

        Long subjectId,

        List<Long> subjectIds) {
}
