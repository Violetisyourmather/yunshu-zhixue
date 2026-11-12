package com.yunshu.zhixue.modules.study.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunshu.zhixue.modules.study.entity.WrongQuestion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 错题本持久层。
 */
@Mapper
public interface WrongQuestionMapper extends BaseMapper<WrongQuestion> {

    /**
     * 查询错题记录（包含逻辑删除行）。
     *
     * <p>错题被移除后再次答错时恢复原行，避免
     * {@code uk_wrong_question(student_id, question_id)} 唯一键冲突。</p>
     */
    @Select("""
            SELECT *
            FROM wrong_question
            WHERE student_id = #{studentId} AND question_id = #{questionId}
            LIMIT 1
            """)
    WrongQuestion findIncludingDeleted(@Param("studentId") long studentId,
                                       @Param("questionId") long questionId);

    /** 恢复已移除的错题，重置错误次数与纠正进度。 */
    @Update("""
            UPDATE wrong_question
            SET delete_flag = 0,
                error_count = 1,
                corrected_count = 0,
                status = 1,
                last_wrong_at = #{lastWrongAt},
                last_answer = #{lastAnswer},
                updated_at = UTC_TIMESTAMP()
            WHERE id = #{id}
            """)
    int restore(@Param("id") long id,
                @Param("lastWrongAt") LocalDateTime lastWrongAt,
                @Param("lastAnswer") String lastAnswer);
}
