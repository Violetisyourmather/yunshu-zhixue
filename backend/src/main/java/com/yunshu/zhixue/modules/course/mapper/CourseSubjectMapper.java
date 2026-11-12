package com.yunshu.zhixue.modules.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunshu.zhixue.modules.course.entity.CourseSubject;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 课程-学科关联持久层。
 */
@Mapper
public interface CourseSubjectMapper extends BaseMapper<CourseSubject> {

    /**
     * 物理删除课程的全部额外学科关联。
     *
     * <p>关联表为纯覆盖数据（类比 role_permission）：逻辑删除会占用
     * {@code uk_course_subject} 唯一键，导致重新关联同一学科时冲突。</p>
     */
    @Delete("DELETE FROM course_subject WHERE course_id = #{courseId}")
    int deleteByCourseId(@Param("courseId") Long courseId);
}
