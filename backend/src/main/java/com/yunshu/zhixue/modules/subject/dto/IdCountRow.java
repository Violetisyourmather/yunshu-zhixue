package com.yunshu.zhixue.modules.subject.dto;

import lombok.Data;

/**
 * 聚合查询结果行：按业务 id 分组后的计数。
 *
 * <p>仅用于学科模块内部的跨表统计（题目 / 课程 / 路线），不对外暴露。</p>
 */
@Data
public class IdCountRow {

    private Long id;

    private Long total;
}
