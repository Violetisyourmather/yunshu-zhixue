package com.yunshu.zhixue.modules.interviewbank.dto;

import lombok.Data;

/**
 * 按主题名称聚合的题目数量。
 */
@Data
public class TopicCountRow {

    private String topic;

    private Long total;
}
