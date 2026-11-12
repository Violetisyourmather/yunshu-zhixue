package com.yunshu.zhixue.modules.interviewbank.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunshu.zhixue.modules.interviewbank.dto.TopicCountRow;
import com.yunshu.zhixue.modules.interviewbank.entity.InterviewQuestion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 面试题库持久层。
 */
@Mapper
public interface InterviewQuestionMapper extends BaseMapper<InterviewQuestion> {

    /** 各主题题目数（按主题名称聚合，未分类不统计）。 */
    @Select("""
            SELECT topic, COUNT(*) AS total
            FROM interview_question
            WHERE delete_flag = 0 AND topic IS NOT NULL AND topic <> ''
            GROUP BY topic
            """)
    List<TopicCountRow> countByTopic();
}
