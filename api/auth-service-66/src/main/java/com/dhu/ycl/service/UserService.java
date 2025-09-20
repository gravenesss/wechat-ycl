package com.dhu.ycl.service;

import com.dhu.ycl.pojo.Users;
import com.dhu.ycl.pojo.bo.RegistLoginBO;

public interface UserService {
    // 判断用户是否存在，如果存在则返回用户信息，否则null
    public Users queryByMobile(String mobile);

    // 创建用户信息，并且返回用户对象
    public Users createUser(RegistLoginBO registLoginBO);
}
