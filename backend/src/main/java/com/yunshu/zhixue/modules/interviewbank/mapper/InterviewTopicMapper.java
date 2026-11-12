package com.yunshu.zhixue.modules.interviewbank.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunshu.zhixue.modules.interviewbank.entity.InterviewTopic;
import org.apache.ibatis.annotations.Mapper;

/**
 * 面试题主题词表持久层。
 */
@Mapper
public interface InterviewTopicMapper extends BaseMapper<InterviewTopic> {
}
