package com.dhu.ycl.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dhu.ycl.base.BaseInfoProperties;
import com.dhu.ycl.exceptions.GraceException;
import com.dhu.ycl.feign.FileMicroServiceFeign;
import com.dhu.ycl.grace.result.ResponseStatusEnum;
import com.dhu.ycl.mapper.UserMapper;
import com.dhu.ycl.pojo.Users;
import com.dhu.ycl.pojo.bo.ModifyUserBO;
import com.dhu.ycl.service.UserService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;


@Service
public class UserServiceImpl extends BaseInfoProperties implements UserService {
    @Resource
    private UserMapper userMapper;

    @Resource
    private FileMicroServiceFeign fileMicroServiceFeign;

    // 获得用户信息
    @Override
    public Users getById(String userId) {
        return userMapper.selectById(userId);
    }

    // 根据微信号（账号）或者手机号精确匹配
    @Override
    public Users getByWechatNumOrMobile(String queryString) {
        QueryWrapper queryWrapper = new QueryWrapper<Users>()
                .eq("wechat_num", queryString).or()
                .eq("mobile", queryString);
        return userMapper.selectOne(queryWrapper);
    }

    // 修改用户基本信息
    @Override
    public void modifyUserInfo(ModifyUserBO userBO) {
        Users pendingUser = new Users();
        String userId = userBO.getUserId();
        String wechatNum = userBO.getWechatNum();
        if (StringUtils.isBlank(userId)) // 没有id更新失败
            GraceException.display(ResponseStatusEnum.USER_INFO_UPDATED_ERROR);
        // 修改微信名的限制：一个用户一年只能修改一次微信名
        if (StringUtils.isNotBlank(wechatNum)) {
            // redis_user_already_update_wechat_num:userId  微信号已被修改，请等待1年后再修改！ 正常就修改了微信名，并加入redis
            String isExist = redis.get(REDIS_USER_ALREADY_UPDATE_WECHAT_NUM + ":" + userId);
            if (StringUtils.isNotBlank(isExist)) {
                GraceException.display(ResponseStatusEnum.WECHAT_NUM_ALREADY_MODIFIED_ERROR);
            } else {
                // 更新微信二维码 并加入redis
                String wechatNumUrl = getQrCodeUrl(wechatNum, userId);
                pendingUser.setWechatNumImg(wechatNumUrl);
                redis.setByDays(REDIS_USER_ALREADY_UPDATE_WECHAT_NUM + ":" + userId, userId, 365);
            }
        }
        pendingUser.setId(userId);
        pendingUser.setUpdatedTime(LocalDateTime.now());

        BeanUtils.copyProperties(userBO, pendingUser);
        userMapper.updateById(pendingUser);
    }

    private String getQrCodeUrl(String wechatNumber, String userId) {
        try {
            return fileMicroServiceFeign.generatorQrCode(wechatNumber, userId);
        } catch (Exception e) {
            return null;
        }
    }
}
