package com.vocab.estimator.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vocab.estimator.entity.UserInfo;

/**
 * User info service interface
 */
public interface UserInfoService extends IService<UserInfo> {
    UserInfo findByStudentCode(String studentCode);
    UserInfo createOrUpdate(UserInfo user);
}
