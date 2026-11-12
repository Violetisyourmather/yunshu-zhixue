package com.yunshu.zhixue.modules.subject.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunshu.zhixue.modules.subject.dto.IdCountRow;
import com.yunshu.zhixue.modules.subject.entity.Subject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 学科持久层。
 *
 * <p>跨表统计（题目 / 课程 / 路线）以只读聚合 SQL 实现：学科是内容体系根节点，
 * 其他内容模块只读依赖，不反向依赖它们的实体。</p>
 */
@Mapper
public interface SubjectMapper extends BaseMapper<Subject> {

    /** 各学科题目数：question → stage → subject。 */
    @Select("""
            SELECT s.subject_id AS id, COUNT(q.id) AS total
            FROM question q
            JOIN stage s ON q.stage_id = s.id
            WHERE q.delete_flag = 0 AND s.delete_flag = 0
            GROUP BY s.subject_id
            """)
    List<IdCountRow> countQuestionsBySubject();

    /** 各阶段题目数。 */
    @Select("""
            SELECT stage_id AS id, COUNT(id) AS total
            FROM question
            WHERE delete_flag = 0
            GROUP BY stage_id
            """)
    List<IdCountRow> countQuestionsByStage();

    /** 各学科关联课程数：主学科 + 关联表，按课程去重。 */
    @Select("""
            SELECT subject_id AS id, COUNT(DISTINCT course_id) AS total
            FROM (
                SELECT id AS course_id, subject_id
                FROM course
                WHERE delete_flag = 0 AND subject_id IS NOT NULL
                UNION
                SELECT cs.course_id, cs.subject_id
                FROM course_subject cs
                JOIN course c ON c.id = cs.course_id AND c.delete_flag = 0
                WHERE cs.delete_flag = 0
            ) t
            GROUP BY subject_id
            """)
    List<IdCountRow> countCoursesBySubject();

    /** 各学科学习路线数。 */
    @Select("""
            SELECT subject_id AS id, COUNT(id) AS total
            FROM learning_route
            WHERE delete_flag = 0
            GROUP BY subject_id
            """)
    List<IdCountRow> countRoutesBySubject();

    /** 各学科上架课程数（主学科 + 关联表，按课程去重），学员路线页学科卡使用。 */
    @Select("""
            SELECT subject_id AS id, COUNT(DISTINCT course_id) AS total
            FROM (
                SELECT id AS course_id, subject_id
                FROM course
                WHERE delete_flag = 0 AND status = 1 AND subject_id IS NOT NULL
                UNION
                SELECT cs.course_id, cs.subject_id
                FROM course_subject cs
                JOIN course c ON c.id = cs.course_id AND c.delete_flag = 0 AND c.status = 1
                WHERE cs.delete_flag = 0
            ) t
            GROUP BY subject_id
            """)
    List<IdCountRow> countPublishedCoursesBySubject();

    /** 全站题目总数（仅统计有效阶段下的有效题目）。 */
    @Select("""
            SELECT COUNT(*)
            FROM question q
            JOIN stage s ON q.stage_id = s.id
            WHERE q.delete_flag = 0 AND s.delete_flag = 0
            """)
    long countAllQuestions();

    /** 指定学科关联课程数（主学科 + 关联表，按课程去重）。 */
    @Select("""
            SELECT COUNT(DISTINCT c.id)
            FROM course c
            WHERE c.delete_flag = 0
              AND (c.subject_id = #{subjectId}
                   OR EXISTS (SELECT 1 FROM course_subject cs
                              WHERE cs.course_id = c.id
                                AND cs.subject_id = #{subjectId}
                                AND cs.delete_flag = 0))
            """)
    long countCourses(@Param("subjectId") Long subjectId);

    /** 指定学科学习路线数。 */
    @Select("""
            SELECT COUNT(*)
            FROM learning_route
            WHERE subject_id = #{subjectId} AND delete_flag = 0
            """)
    long countRoutes(@Param("subjectId") Long subjectId);
}
