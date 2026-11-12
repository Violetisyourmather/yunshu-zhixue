package com.yunshu.zhixue.modules.stats.dto;

import lombok.Data;

/**
 * 热门学科聚合行（subject + stage + question）。
 */
@Data
public class HotSubjectRow {

    private Long subjectId;

    private String subjectName;

    private String icon;

    private Long stageCount;

    private Long questionCount;
}
