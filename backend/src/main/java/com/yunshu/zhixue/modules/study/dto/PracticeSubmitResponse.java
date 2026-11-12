package com.yunshu.zhixue.modules.study.dto;

import java.util.List;

/**
 * P-P02 提交作答响应。
 */
public record PracticeSubmitResponse(List<PracticeResultItem> results) {
}
