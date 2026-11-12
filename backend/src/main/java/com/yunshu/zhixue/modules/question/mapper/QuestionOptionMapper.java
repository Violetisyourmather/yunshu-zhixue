package com.yunshu.zhixue.modules.question.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunshu.zhixue.modules.question.entity.QuestionOption;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 题目选项持久层。
 */
@Mapper
public interface QuestionOptionMapper extends BaseMapper<QuestionOption> {

    /** 物理删除题目的全部选项，避免逻辑删除行占用唯一键。 */
    @Delete("DELETE FROM question_option WHERE question_id = #{questionId}")
    int deleteByQuestionId(@Param("questionId") Long questionId);
}
