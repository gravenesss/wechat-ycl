package com.dhu.ycl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dhu.ycl.pojo.Users;

// UserMapper 接口只是简单继承了 BaseMapper<Users>，没有添加任何自定义方法；
// 因此这个 XML 文件不是必须的，目前放着的原因是方便后续添加自定义方法
public interface UserMapper extends BaseMapper<Users> {
}
