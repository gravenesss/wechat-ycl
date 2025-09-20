package com.dhu.ycl.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dhu.ycl.base.BaseInfoProperties;
import com.dhu.ycl.enums.Sex;
import com.dhu.ycl.feign.FileMicroServiceFeign;
import com.dhu.ycl.mapper.UsersMapper;
import com.dhu.ycl.pojo.Users;
import com.dhu.ycl.pojo.bo.RegistLoginBO;
import com.dhu.ycl.utils.DesensitizationUtil;
import com.dhu.ycl.utils.LocalDateUtils;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {
    private static final String USER_FACE1 = "";
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    @Resource
    private UsersMapper usersMapper;

    @Override
    public Users queryByMobile(String mobile) {
        return usersMapper.selectOne(new QueryWrapper<Users>().eq("mobile", mobile));
    }

    @Override
    public Users createUser(RegistLoginBO registLoginBO) {
        String mobile = registLoginBO.getMobile();
        String nickname = registLoginBO.getNickname();

        Users user = new Users();
        user.setMobile(mobile);
        // 微信号
        String uuid = UUID.randomUUID().toString();
        String uuidStr[] = uuid.split("-");
        String wechatNum = "wx" + uuidStr[0] + uuidStr[1];
        user.setWechatNum(wechatNum);
        // QR 二维码
        String wechatNumUrl = this.getQrCodeUrl(wechatNum, BaseInfoProperties.TEMP_STRING);
        user.setWechatNumImg(wechatNumUrl);
        // 昵称：用户138****1234: DesensitizationUtil 脱敏工具
        if (StringUtils.isBlank(nickname)) {
            user.setNickname("用户" + DesensitizationUtil.commonDisplay(mobile));
        } else {
            user.setNickname(nickname);
        }
        // ======== 以下设置的都是一些空值或默认值 ========
        user.setRealName("");
        // 性别：默认保密
        user.setSex(Sex.secret.type);
        // 头像
        user.setFace(USER_FACE1);
        // 好友圈背景
        user.setFriendCircleBg(USER_FACE1);
        // 邮箱
        user.setEmail("");
        // 生日
        user.setBirthday(LocalDateUtils.parseLocalDate("1980-01-01", LocalDateUtils.DATE_PATTERN));
        // 国家-省-市-区
        user.setCountry("中国");
        user.setProvince("");
        user.setCity("");
        user.setDistrict("");
        // 创建时间-更新时间
        user.setCreatedTime(LocalDateTime.now());
        user.setUpdatedTime(LocalDateTime.now());
        usersMapper.insert(user);

        return user;
    }

    @Resource
    private FileMicroServiceFeign fileMicroServiceFeign;

    private String getQrCodeUrl(String wechatNumber, String userId) {
        try {
            return fileMicroServiceFeign.generatorQrCode(wechatNumber, userId);
        } catch (Exception e) {
            log.error("生成二维码失败", e);
            return null;
        }
    }
}
