package com.yunshu.zhixue.modules.route.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunshu.zhixue.modules.route.entity.RouteCourseCompletion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 学员课程完成记录持久层。
 */
@Mapper
public interface RouteCourseCompletionMapper extends BaseMapper<RouteCourseCompletion> {

    /** 当前学员已完成的所有路线课程 id。 */
    @Select("""
            SELECT route_course_id
            FROM route_course_completion
            WHERE student_id = #{studentId} AND delete_flag = 0
            """)
    List<Long> selectCompletedRouteCourseIds(@Param("studentId") long studentId);

    /**
     * 查询完成记录（包含逻辑删除行）。
     *
     * <p>取消完成后重新勾选需恢复原行，避免
     * {@code uk_route_course_completion} 唯一键冲突。</p>
     */
    @Select("""
            SELECT *
            FROM route_course_completion
            WHERE student_id = #{studentId} AND route_course_id = #{routeCourseId}
            LIMIT 1
            """)
    RouteCourseCompletion findIncludingDeleted(@Param("studentId") long studentId,
                                               @Param("routeCourseId") long routeCourseId);

    /** 恢复已逻辑删除的完成记录并刷新完成时间。 */
    @Update("""
            UPDATE route_course_completion
            SET delete_flag = 0,
                completed_at = #{completedAt},
                updated_at = UTC_TIMESTAMP()
            WHERE id = #{id}
            """)
    int restore(@Param("id") long id, @Param("completedAt") LocalDateTime completedAt);
}
