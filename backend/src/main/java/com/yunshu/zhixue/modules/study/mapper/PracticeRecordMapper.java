package com.yunshu.zhixue.modules.study.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunshu.zhixue.modules.study.entity.PracticeRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 刷题作答明细持久层。
 */
@Mapper
public interface PracticeRecordMapper extends BaseMapper<PracticeRecord> {
}
