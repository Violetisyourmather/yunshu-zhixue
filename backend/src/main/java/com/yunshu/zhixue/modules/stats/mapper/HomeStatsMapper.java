package com.yunshu.zhixue.modules.stats.mapper;

import com.yunshu.zhixue.modules.stats.dto.DailyPracticeCountRow;
import com.yunshu.zhixue.modules.stats.dto.HotSubjectRow;
import com.yunshu.zhixue.modules.stats.dto.PracticeSummaryRow;
import com.yunshu.zhixue.modules.stats.dto.StudentGuideRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 首页统计只读查询（P-H01 ~ P-H03）。
 *
 * <p>统计模块自有聚合 SQL，不依赖其他模块的 Mapper；所有查询均以明细表为事实来源，
 * 在查询时聚合，不落冗余统计表。</p>
 */
@Mapper
public interface HomeStatsMapper {

    /** P-H01 注册学员数：未删除学员。 */
    @Select("SELECT COUNT(*) FROM student WHERE delete_flag = 0")
    long countStudents();

    /** P-H01 精选面试题：已上架且所属学科未删除。 */
    @Select("""
            SELECT COUNT(*)
            FROM interview_question iq
            JOIN subject s ON s.id = iq.subject_id AND s.delete_flag = 0
            WHERE iq.delete_flag = 0 AND iq.status = 1
            """)
    long countPublishedInterviewQuestions();

    /** P-H01 AI 面试次数：未删除学员的已完成面试场次。 */
    @Select("""
            SELECT COUNT(*)
            FROM interview_record ir
            JOIN student st ON st.id = ir.student_id AND st.delete_flag = 0
            WHERE ir.delete_flag = 0 AND ir.completed_at IS NOT NULL
            """)
    long countCompletedInterviews();

    /**
     * P-H02 热门学科：按题目数倒序取前 N 个。
     *
     * <p>阶段只统计启用且未删除项；题目只统计阶段下未删除项，口径与学科管理一致。</p>
     */
    @Select("""
            SELECT s.id AS subjectId,
                   s.name AS subjectName,
                   s.icon AS icon,
                   COUNT(DISTINCT st.id) AS stageCount,
                   COUNT(DISTINCT q.id) AS questionCount
            FROM subject s
            LEFT JOIN stage st
                   ON st.subject_id = s.id
                  AND st.delete_flag = 0
                  AND st.status = 1
            LEFT JOIN question q
                   ON q.stage_id = st.id
                  AND q.delete_flag = 0
            WHERE s.delete_flag = 0
            GROUP BY s.id, s.name, s.icon, s.sort_order
            ORDER BY questionCount DESC, s.sort_order ASC, s.id ASC
            LIMIT #{limit}
            """)
    List<HotSubjectRow> selectHotSubjects(@Param("limit") int limit);

    /** P-H03 学员账号状态与新手引导状态。 */
    @Select("""
            SELECT status AS status, guide_finished AS guideFinished
            FROM student
            WHERE id = #{studentId} AND delete_flag = 0
            """)
    StudentGuideRow selectStudentGuide(@Param("studentId") long studentId);

    /**
     * P-H03 刷题汇总：总记录数 + 客观题作答数 / 答对数。
     *
     * <p>题目使用 LEFT JOIN，题目被逻辑删除后历史作答仍计入总数与正确率。</p>
     */
    @Select("""
            SELECT COUNT(pr.id) AS total,
                   COALESCE(SUM(CASE WHEN q.type IN (1, 2, 3) THEN 1 ELSE 0 END), 0) AS objectiveTotal,
                   COALESCE(SUM(CASE WHEN q.type IN (1, 2, 3) AND pr.is_correct = 1 THEN 1 ELSE 0 END), 0)
                       AS objectiveCorrect
            FROM practice_record pr
            LEFT JOIN question q ON q.id = pr.question_id
            WHERE pr.student_id = #{studentId} AND pr.delete_flag = 0
            """)
    PracticeSummaryRow selectPracticeSummary(@Param("studentId") long studentId);

    /** P-H03 有作答记录的上海自然日，倒序。 */
    @Select("""
            SELECT DATE_FORMAT(DATE_ADD(practiced_at, INTERVAL 8 HOUR), '%Y-%m-%d') AS practiceDate
            FROM practice_record
            WHERE student_id = #{studentId} AND delete_flag = 0
            GROUP BY practiceDate
            ORDER BY practiceDate DESC
            """)
    List<String> selectPracticeDatesDesc(@Param("studentId") long studentId);

    /** P-H03 指定时间之后的每日练习量，按上海自然日聚合。 */
    @Select("""
            SELECT DATE_FORMAT(DATE_ADD(practiced_at, INTERVAL 8 HOUR), '%Y-%m-%d') AS practiceDate,
                   COUNT(*) AS total
            FROM practice_record
            WHERE student_id = #{studentId}
              AND delete_flag = 0
              AND practiced_at >= #{startUtc}
            GROUP BY practiceDate
            ORDER BY practiceDate ASC
            """)
    List<DailyPracticeCountRow> selectDailyPracticeCounts(@Param("studentId") long studentId,
                                                          @Param("startUtc") LocalDateTime startUtc);
}
