package com.yunshu.zhixue.modules.interviewbank.dto;

import java.util.List;

/**
 * 面试题导入预览暂存在 Redis 中的完整载荷。
 */
public record InterviewImportPayload(String fileName, List<InterviewImportRow> rows) {
}
