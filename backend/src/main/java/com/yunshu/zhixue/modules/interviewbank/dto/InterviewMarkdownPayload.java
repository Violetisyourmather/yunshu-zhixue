package com.yunshu.zhixue.modules.interviewbank.dto;

import java.util.List;

/**
 * Markdown 导入预览暂存内容（Redis，TTL 30min）。
 */
public record InterviewMarkdownPayload(String fileName, List<InterviewMarkdownQuestion> questions) {
}
