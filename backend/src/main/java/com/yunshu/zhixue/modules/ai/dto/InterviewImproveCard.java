package com.yunshu.zhixue.modules.ai.dto;

/**
 * 面试报告能力提升卡。
 *
 * @param type bank / route / interview，前端据此生成跳转
 */
public record InterviewImproveCard(String title, String content, String type) {
}
