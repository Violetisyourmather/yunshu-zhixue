package com.yunshu.zhixue.modules.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunshu.zhixue.modules.course.entity.Course;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 课程持久层。
 */
@Mapper
public interface CourseMapper extends BaseMapper<Course> {

    /** 课程总时长（分钟）。 */
    @Select("SELECT COALESCE(SUM(duration_minutes), 0) FROM course WHERE delete_flag = 0")
    long sumDurationMinutes();

    /** 关联学科数：主学科 + 额外学科，排除「其他学科」与已删除学科。 */
    @Select("""
            SELECT COUNT(DISTINCT t.subject_id)
            FROM (
                SELECT subject_id
                FROM course
                WHERE delete_flag = 0 AND subject_id IS NOT NULL
                UNION
                SELECT cs.subject_id
                FROM course_subject cs
                JOIN course c ON c.id = cs.course_id AND c.delete_flag = 0
                WHERE cs.delete_flag = 0
            ) t
            JOIN subject s ON s.id = t.subject_id AND s.delete_flag = 0
            """)
    long countDistinctSubjects();

    /** 课程是否被有效学习路线引用。 */
    @Select("""
            SELECT COUNT(*)
            FROM route_course rc
            JOIN learning_route lr ON lr.id = rc.route_id AND lr.delete_flag = 0
            WHERE rc.course_id = #{courseId} AND rc.delete_flag = 0
            """)
    long countRouteReferences(@Param("courseId") Long courseId);
}
