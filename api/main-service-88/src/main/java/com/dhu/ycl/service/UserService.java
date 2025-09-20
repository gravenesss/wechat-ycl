package com.dhu.ycl.service;

import com.dhu.ycl.pojo.Users;
import com.dhu.ycl.pojo.bo.ModifyUserBO;

public interface UserService {
    // 修改用户基本信息
    void modifyUserInfo(ModifyUserBO userBO);

    // 获得用户信息
    Users getById(String userId);

    // 根据微信号（账号）或者手机号精确匹配
    Users getByWechatNumOrMobile(String queryString);
}
