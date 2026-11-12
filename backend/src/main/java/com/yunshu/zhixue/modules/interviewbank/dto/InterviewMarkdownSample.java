package com.yunshu.zhixue.modules.interviewbank.dto;

/**
 * Markdown 解析预览样例（前 3 道）。
 *
 * @param seq           题号（按标题顺序）
 * @param title         题干（Markdown 标题）
 * @param answerPreview 参考答案摘要
 * @param line          标题所在行号
 */
public record InterviewMarkdownSample(int seq, String title, String answerPreview, int line) {
}
