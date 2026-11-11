package com.yunshu.zhixue.modules.student.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunshu.zhixue.modules.student.entity.Student;
import org.apache.ibatis.annotations.Mapper;

/** 学员表 Mapper（03 §4.1）。 */
@Mapper
public interface StudentMapper extends BaseMapper<Student> {
}
