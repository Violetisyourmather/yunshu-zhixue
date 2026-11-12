package com.yunshu.zhixue.modules.interviewbank.dto;

/**
 * Markdown 解析出的单道面试题（Redis 暂存结构）。
 */
public record InterviewMarkdownQuestion(int seq, String title, String answer, int line) {
}
