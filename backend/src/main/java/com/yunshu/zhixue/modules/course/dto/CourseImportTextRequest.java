package com.yunshu.zhixue.modules.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A-C09 粘贴文本行导入预览。
 *
 * <p>每行一门课程，字段用 Tab、竖线或逗号分隔，顺序为：
 * 课程名称 / 所属学科 / 来源 / 时长（分钟） / BV号 / 封面 / 播放链接。</p>
 */
public record CourseImportTextRequest(
        @NotBlank(message = "请粘贴课程文本")
        @Size(max = 1_000_000, message = "粘贴内容不能超过 100 万个字符")
        String text) {
}
