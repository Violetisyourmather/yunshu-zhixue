package com.yunshu.zhixue.modules.question.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunshu.zhixue.modules.question.dto.QuestionStatRow;
import com.yunshu.zhixue.modules.question.entity.Question;
import com.yunshu.zhixue.modules.subject.dto.IdCountRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * 题库题目持久层。
 */
@Mapper
public interface QuestionMapper extends BaseMapper<Question> {

    /** 批量统计作答次数 / 正确次数（详情与列表共用）。 */
    @Select("""
            <script>
            SELECT question_id AS id, COUNT(*) AS total, COALESCE(SUM(is_correct), 0) AS correct
            FROM practice_record
            WHERE delete_flag = 0
              AND question_id IN
              <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
            GROUP BY question_id
            </script>
            """)
    List<QuestionStatRow> selectPracticeStats(@Param("ids") Collection<Long> ids);

    /** 是否被练习记录引用。 */
    @Select("SELECT COUNT(*) FROM practice_record WHERE question_id = #{questionId} AND delete_flag = 0")
    long countPracticeReferences(@Param("questionId") Long questionId);

    /** 是否被错题本引用。 */
    @Select("SELECT COUNT(*) FROM wrong_question WHERE question_id = #{questionId} AND delete_flag = 0")
    long countWrongReferences(@Param("questionId") Long questionId);

    /** 各阶段已上架题目数，学员题库阶段卡使用。 */
    @Select("""
            SELECT stage_id AS id, COUNT(*) AS total
            FROM question
            WHERE delete_flag = 0 AND status = 1
            GROUP BY stage_id
            """)
    List<IdCountRow> countPublishedByStage();

    /** 指定题目是否已在当前学员的错题本中（未移除状态）。 */
    @Select("""
            SELECT COUNT(*)
            FROM wrong_question
            WHERE student_id = #{studentId}
              AND question_id = #{questionId}
              AND delete_flag = 0
            """)
    long countInWrongBook(@Param("studentId") Long studentId,
                          @Param("questionId") Long questionId);
}
