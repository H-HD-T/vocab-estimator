package com.vocab.estimator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vocab.estimator.entity.UserInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * User info mapper
 */
@Mapper
public interface UserInfoMapper extends BaseMapper<UserInfo> {
    
    /** Find user by student code */
    @Select("SELECT * FROM user_info WHERE student_code = #{studentCode}")
    UserInfo findByStudentCode(String studentCode);
    
    /** Find users by type */
    @Select("SELECT * FROM user_info WHERE student_type = #{studentType}")
    List<UserInfo> findByStudentType(String studentType);
}
