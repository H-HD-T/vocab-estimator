package com.vocab.estimator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vocab.estimator.entity.TestRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * Test record mapper
 */
@Mapper
public interface TestRecordMapper extends BaseMapper<TestRecord> {
    
    /** Get all test records for a user, ordered by time */
    @Select("SELECT * FROM test_record WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<TestRecord> findByUserId(@Param("userId") Long userId);
    
    /** Get records by test type */
    @Select("SELECT * FROM test_record WHERE test_type = #{testType}")
    List<TestRecord> findByTestType(@Param("testType") String testType);
}
