package com.vocab.estimator.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vocab.estimator.entity.UserInfo;
import com.vocab.estimator.mapper.UserInfoMapper;
import com.vocab.estimator.service.UserInfoService;
import org.springframework.stereotype.Service;

/**
 * User info service implementation
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

    @Override
    public UserInfo findByStudentCode(String studentCode) {
        return baseMapper.findByStudentCode(studentCode);
    }

    @Override
    public UserInfo createOrUpdate(UserInfo user) {
        UserInfo existing = findByStudentCode(user.getStudentCode());
        if (existing != null) {
            user.setId(existing.getId());
            updateById(user);
            return user;
        }
        save(user);
        return user;
    }
}
