package com.yunshu.zhixue.modules.student.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunshu.zhixue.modules.student.dto.StudentCountRow;
import com.yunshu.zhixue.modules.student.dto.StudentInterviewRow;
import com.yunshu.zhixue.modules.student.dto.StudentPracticeStatRow;
import com.yunshu.zhixue.modules.student.entity.Student;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/** 学员表 Mapper（03 §4.1）。 */
@Mapper
public interface StudentMapper extends BaseMapper<Student> {

    /** 各学员刷题总数 / 正确数（列表、详情、导出共用）。 */
    @Select("""
            <script>
            SELECT student_id AS id, COUNT(*) AS total, COALESCE(SUM(is_correct), 0) AS correct
            FROM practice_record
            WHERE delete_flag = 0
              AND student_id IN
              <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
            GROUP BY student_id
            </script>
            """)
    List<StudentPracticeStatRow> selectPracticeStats(@Param("ids") Collection<Long> ids);

    /** 各学员累计学习天数：有作答记录的不同上海自然日数。 */
    @Select("""
            <script>
            SELECT student_id AS id,
                   COUNT(DISTINCT DATE(DATE_ADD(practiced_at, INTERVAL 8 HOUR))) AS total
            FROM practice_record
            WHERE delete_flag = 0
              AND student_id IN
              <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
            GROUP BY student_id
            </script>
            """)
    List<StudentCountRow> selectLearningDays(@Param("ids") Collection<Long> ids);

    /** 各学员 AI 面试次数。 */
    @Select("""
            <script>
            SELECT student_id AS id, COUNT(*) AS total
            FROM interview_record
            WHERE delete_flag = 0
              AND student_id IN
              <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
            GROUP BY student_id
            </script>
            """)
    List<StudentCountRow> selectInterviewCounts(@Param("ids") Collection<Long> ids);

    /** 各学员已学课程数（路线中手动标记完成）。 */
    @Select("""
            <script>
            SELECT student_id AS id, COUNT(*) AS total
            FROM route_course_completion
            WHERE delete_flag = 0
              AND student_id IN
              <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
            GROUP BY student_id
            </script>
            """)
    List<StudentCountRow> selectCompletionCounts(@Param("ids") Collection<Long> ids);

    /** 学员面试记录总数。 */
    @Select("SELECT COUNT(*) FROM interview_record WHERE student_id = #{studentId} AND delete_flag = 0")
    long countInterviews(@Param("studentId") Long studentId);

    /** 学员错题本题目数（含待复习与已掌握，不含已移除）。 */
    @Select("SELECT COUNT(*) FROM wrong_question WHERE student_id = #{studentId} AND delete_flag = 0")
    long countWrongQuestions(@Param("studentId") Long studentId);

    /** 学员面试记录分页。 */
    @Select("""
            SELECT ir.id, ir.subject_id AS subjectId, s.name AS subjectName,
                   ir.difficulty, ir.question_count AS questionCount,
                   ir.overall_score AS overallScore, ir.total_duration AS totalDuration,
                   ir.started_at AS startedAt, ir.completed_at AS completedAt
            FROM interview_record ir
            LEFT JOIN subject s ON s.id = ir.subject_id AND s.delete_flag = 0
            WHERE ir.student_id = #{studentId} AND ir.delete_flag = 0
            ORDER BY ir.completed_at DESC, ir.id DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<StudentInterviewRow> selectInterviews(@Param("studentId") Long studentId,
                                               @Param("offset") long offset,
                                               @Param("limit") long limit);
}
