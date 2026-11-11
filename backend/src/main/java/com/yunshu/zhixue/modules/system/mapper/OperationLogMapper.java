package com.yunshu.zhixue.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunshu.zhixue.modules.system.entity.OperationLog;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作日志持久层。
 *
 * <p>{@code operation_log} 为审计表，仅新增/查询；清空为物理删除（03 §6），
 * 因 MyBatis-Plus 的全表删除拦截，使用带恒真条件的显式 SQL。</p>
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {

    /** A-G04 清空全部日志（物理删除）。 */
    @Delete("DELETE FROM operation_log WHERE id > 0")
    int deleteAll();
}
