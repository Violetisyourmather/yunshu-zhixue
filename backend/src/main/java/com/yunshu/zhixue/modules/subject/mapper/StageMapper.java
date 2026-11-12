package com.yunshu.zhixue.modules.subject.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunshu.zhixue.modules.subject.entity.Stage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 阶段持久层。
 */
@Mapper
public interface StageMapper extends BaseMapper<Stage> {

    /** 阶段下有效题目数（删除拦截与列表展示共用）。 */
    @Select("""
            SELECT COUNT(*)
            FROM question
            WHERE stage_id = #{stageId} AND delete_flag = 0
            """)
    long countQuestions(@Param("stageId") Long stageId);

    /** 学科下所有阶段的有效题目数。 */
    @Select("""
            SELECT COUNT(*)
            FROM question q
            JOIN stage s ON q.stage_id = s.id
            WHERE s.subject_id = #{subjectId}
              AND q.delete_flag = 0
              AND s.delete_flag = 0
            """)
    long countQuestionsBySubject(@Param("subjectId") Long subjectId);
}
