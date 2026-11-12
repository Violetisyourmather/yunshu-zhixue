package com.yunshu.zhixue.modules.route.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunshu.zhixue.modules.route.entity.RouteCourse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 路线-课程关联持久层。
 */
@Mapper
public interface RouteCourseMapper extends BaseMapper<RouteCourse> {

    /**
     * 查询关联行（包含逻辑删除行）。
     *
     * <p>用于重新添加同一课程时恢复原行，避免 {@code uk_route_course} 唯一键冲突；
     * 自定义 SQL 不经过 MyBatis-Plus 逻辑删除过滤。</p>
     */
    @Select("""
            SELECT *
            FROM route_course
            WHERE route_id = #{routeId} AND course_id = #{courseId}
            LIMIT 1
            """)
    RouteCourse findIncludingDeleted(@Param("routeId") Long routeId,
                                     @Param("courseId") Long courseId);

    /** 恢复已逻辑删除的关联行并更新排序。 */
    @Update("""
            UPDATE route_course
            SET delete_flag = 0,
                sort_order = #{sortOrder},
                updated_at = UTC_TIMESTAMP()
            WHERE id = #{id}
            """)
    int restore(@Param("id") Long id, @Param("sortOrder") Integer sortOrder);
}
