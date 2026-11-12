package com.yunshu.zhixue.modules.interviewbank.dto;

import java.util.List;

/**
 * Markdown 手工导入预览。
 *
 * @param token         确认导入令牌（Redis 暂存，TTL 30min）
 * @param fileName      文件名
 * @param fileSize      文件字节数
 * @param questionCount 解析出的题目数
 * @param samples       前 3 道样例
 */
public record InterviewMarkdownPreview(
        String token,
        String fileName,
        long fileSize,
        int questionCount,
        List<InterviewMarkdownSample> samples) {
}
