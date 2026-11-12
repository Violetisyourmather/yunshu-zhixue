package com.yunshu.zhixue.modules.interview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunshu.zhixue.modules.interview.entity.InterviewReport;
import org.apache.ibatis.annotations.Mapper;

/**
 * 面试报告持久层。
 */
@Mapper
public interface InterviewReportMapper extends BaseMapper<InterviewReport> {
}
