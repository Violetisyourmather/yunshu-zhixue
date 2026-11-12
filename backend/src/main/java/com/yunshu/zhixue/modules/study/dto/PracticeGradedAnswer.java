package com.yunshu.zhixue.modules.study.dto;

/**
 * 服务端内部使用的已判分作答，交给持久化服务落库。
 */
public record PracticeGradedAnswer(
        long questionId,
        boolean correct,
        Integer durationSeconds,
        String studentAnswer) {
}
