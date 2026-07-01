package com.vocab.estimator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vocab.estimator.entity.BatchTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * Batch task mapper
 */
@Mapper
public interface BatchTaskMapper extends BaseMapper<BatchTask> {
    
    @Select("SELECT * FROM batch_task ORDER BY create_time DESC")
    List<BatchTask> findAllOrderByTime();
}
