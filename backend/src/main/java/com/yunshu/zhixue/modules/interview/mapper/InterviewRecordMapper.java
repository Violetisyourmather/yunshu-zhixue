package com.yunshu.zhixue.modules.interview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunshu.zhixue.modules.interview.entity.InterviewRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * 面试记录持久层。
 */
@Mapper
public interface InterviewRecordMapper extends BaseMapper<InterviewRecord> {

    /** 同方向时间上的上一条面试记录（报告趋势对比）。 */
    @Select("""
            SELECT *
            FROM interview_record
            WHERE student_id = #{studentId}
              AND subject_id = #{subjectId}
              AND delete_flag = 0
              AND (completed_at < #{completedAt}
                   OR (completed_at = #{completedAt} AND id < #{recordId}))
            ORDER BY completed_at DESC, id DESC
            LIMIT 1
            """)
    InterviewRecord selectPrevious(@Param("studentId") long studentId,
                                   @Param("subjectId") long subjectId,
                                   @Param("completedAt") LocalDateTime completedAt,
                                   @Param("recordId") long recordId);
}
