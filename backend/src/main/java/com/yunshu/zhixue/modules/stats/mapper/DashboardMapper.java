package com.yunshu.zhixue.modules.stats.mapper;

import com.yunshu.zhixue.modules.stats.dto.DailyCountRow;
import com.yunshu.zhixue.modules.stats.dto.QuestionDistributionItem;
import com.yunshu.zhixue.modules.stats.dto.RecentActivityItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端数据看板只读查询（A-D01 ~ A-D04）。
 *
 * <p>与首页统计一致：统计模块自有聚合 SQL，以明细表为事实来源、查询时聚合；
 * 不直接注入其他模块的 Mapper。时间参数均以 UTC 传入。</p>
 */
@Mapper
public interface DashboardMapper {

    /* ---------- A-D01 统计卡 ---------- */

    @Select("SELECT COUNT(*) FROM subject WHERE delete_flag = 0")
    long countSubjects();

    @Select("SELECT COUNT(*) FROM subject WHERE delete_flag = 0 AND created_at >= #{monthStart}")
    long countSubjectsSince(@Param("monthStart") LocalDateTime monthStart);

    @Select("SELECT COUNT(*) FROM question WHERE delete_flag = 0")
    long countQuestions();

    @Select("SELECT COUNT(*) FROM question WHERE delete_flag = 0 AND created_at >= #{monthStart}")
    long countQuestionsSince(@Param("monthStart") LocalDateTime monthStart);

    @Select("SELECT COUNT(*) FROM student WHERE delete_flag = 0")
    long countStudents();

    @Select("SELECT COUNT(*) FROM student WHERE delete_flag = 0 AND created_at >= #{monthStart}")
    long countStudentsSince(@Param("monthStart") LocalDateTime monthStart);

    @Select("""
            SELECT COUNT(*)
            FROM interview_record ir
            JOIN student st ON st.id = ir.student_id AND st.delete_flag = 0
            WHERE ir.delete_flag = 0 AND ir.completed_at IS NOT NULL
            """)
    long countCompletedInterviews();

    @Select("""
            SELECT COUNT(*)
            FROM interview_record ir
            JOIN student st ON st.id = ir.student_id AND st.delete_flag = 0
            WHERE ir.delete_flag = 0
              AND ir.completed_at IS NOT NULL
              AND ir.completed_at >= #{monthStart}
            """)
    long countCompletedInterviewsSince(@Param("monthStart") LocalDateTime monthStart);

    /* ---------- A-D02 用户增长（日粒度，上海自然日） ---------- */

    @Select("""
            SELECT DATE_FORMAT(DATE_ADD(created_at, INTERVAL 8 HOUR), '%Y-%m-%d') AS day,
                   COUNT(*) AS total
            FROM student
            WHERE delete_flag = 0
              AND created_at >= #{startUtc}
              AND created_at < #{endUtc}
            GROUP BY day
            ORDER BY day ASC
            """)
    List<DailyCountRow> selectDailyRegistrations(@Param("startUtc") LocalDateTime startUtc,
                                                 @Param("endUtc") LocalDateTime endUtc);

    /* ---------- A-D03 题目学科分布 ---------- */

    @Select("""
            SELECT s.id AS subjectId,
                   s.name AS subjectName,
                   COUNT(q.id) AS questionCount
            FROM subject s
            LEFT JOIN stage st
                   ON st.subject_id = s.id
                  AND st.delete_flag = 0
            LEFT JOIN question q
                   ON q.stage_id = st.id
                  AND q.delete_flag = 0
            WHERE s.delete_flag = 0
            GROUP BY s.id, s.name, s.sort_order
            ORDER BY questionCount DESC, s.sort_order ASC, s.id ASC
            """)
    List<QuestionDistributionItem> selectQuestionDistribution();

    /* ---------- A-D04 最近活动 ---------- */

    @Select("""
            SELECT id,
                   username,
                   action,
                   target_type AS targetType,
                   target_name AS targetName,
                   ip,
                   created_at AS createdAt
            FROM operation_log
            ORDER BY created_at DESC, id DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<RecentActivityItem> selectRecentActivities(@Param("limit") long limit,
                                                    @Param("offset") long offset);

    @Select("SELECT COUNT(*) FROM operation_log")
    long countActivities();
}
