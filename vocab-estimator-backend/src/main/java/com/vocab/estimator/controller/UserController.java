package com.vocab.estimator.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vocab.estimator.common.Result;
import com.vocab.estimator.entity.UserInfo;
import com.vocab.estimator.service.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserInfoService userInfoService;

    @PostMapping("/register")
    public Result<UserInfo> register(@RequestBody UserInfo user) {
        return Result.success(userInfoService.createOrUpdate(user));
    }

    @GetMapping("/{id}")
    public Result<UserInfo> getById(@PathVariable Long id) {
        return Result.success(userInfoService.getById(id));
    }

    @GetMapping("/page")
    public Result<Page<UserInfo>> page(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return Result.success(userInfoService.page(new Page<>(page, size)));
    }

    @PutMapping
    public Result<UserInfo> update(@RequestBody UserInfo user) {
        userInfoService.updateById(user);
        return Result.success(user);
    }
}
