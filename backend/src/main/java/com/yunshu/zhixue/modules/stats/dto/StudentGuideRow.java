package com.yunshu.zhixue.modules.stats.dto;

import lombok.Data;

/**
 * 学员账号状态与新手引导状态查询行。
 */
@Data
public class StudentGuideRow {

    /** 1 已启用 / 0 已禁用。 */
    private Integer status;

    /** 0 未完成 / 1 已完成。 */
    private Integer guideFinished;
}
