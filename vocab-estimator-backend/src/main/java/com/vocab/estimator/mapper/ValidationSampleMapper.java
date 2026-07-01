package com.vocab.estimator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vocab.estimator.entity.ValidationSample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface ValidationSampleMapper extends BaseMapper<ValidationSample> {
    @Select("SELECT * FROM validation_sample ORDER BY create_time ASC")
    List<ValidationSample> findAllOrderByTime();

    @Select("SELECT COUNT(*) FROM validation_sample")
    int countAll();
}
