package com.yunshu.zhixue.modules.interview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunshu.zhixue.modules.interview.entity.InterviewQuestionScore;
import org.apache.ibatis.annotations.Mapper;

/**
 * 面试逐题评分持久层。
 */
@Mapper
public interface InterviewQuestionScoreMapper extends BaseMapper<InterviewQuestionScore> {
}
