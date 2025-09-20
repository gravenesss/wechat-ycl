package com.dhu.ycl.controller;

import com.dhu.ycl.base.BaseInfoProperties;
import com.dhu.ycl.grace.result.GraceJSONResult;
import com.dhu.ycl.pojo.Users;
import com.dhu.ycl.pojo.bo.RegistLoginBO;
import com.dhu.ycl.pojo.vo.UsersVO;
import com.dhu.ycl.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Preconditions;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/userAuth")
public class UserController extends BaseInfoProperties {
    private static final String MOBILE_SMSCODE_BEFORE = MOBILE_SMSCODE + ":60:";
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private UserService userService;

    @RequestMapping("/getSMSCode")
    public GraceJSONResult getSMSCode(String mobile, HttpServletRequest request) {
        if (StringUtils.isBlank(mobile)) {
            return GraceJSONResult.errorMsg("手机号不能为空");
        }
        // 1、每个手机号60秒只能获取一次验证码
        String beforeGetCode = MOBILE_SMSCODE_BEFORE + mobile;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(beforeGetCode))) {
            return GraceJSONResult.errorMsg("60秒内不能重复获取验证码");
        }
        stringRedisTemplate.opsForValue().setIfAbsent(beforeGetCode, "done", 60, TimeUnit.SECONDS);
        // 2、开始发送验证码
        String smsCode = String.valueOf((int) ((Math.random() * 9 + 1) * 100000));
        log.info("smsCode:{}", smsCode);
        // 3、验证码设置为10分钟有效。mobile:smscode: 手机号，验证码
        stringRedisTemplate.opsForValue().set(MOBILE_SMSCODE + ":" + mobile, smsCode, 10, TimeUnit.MINUTES);
        return GraceJSONResult.ok();
    }

    @PostMapping("/register")
    public GraceJSONResult register(@RequestBody @Valid RegistLoginBO registLoginBO, HttpServletRequest request) {
        String mobile = registLoginBO.getMobile();
        String redisSmsCode = stringRedisTemplate.opsForValue().get(MOBILE_SMSCODE + ":" + mobile);
        log.info("注册：inputSmsCode:{}, redisSmsCode:{}", registLoginBO.getSmsCode(), redisSmsCode);
        // 1、校验验证码
        GraceJSONResult check = checkSmsCode(registLoginBO.getSmsCode(), redisSmsCode);
        if (check != null){
            return check;
        }
        // 2、查看用户手机号是否被使用: 2)为空则进行注册，1)否则返回错误-被全局异常处理 @see com.dhu.ycl.exceptions.GraceExceptionHandler
        Users user = userService.queryByMobile(mobile);
        if (user != null) {
            return GraceJSONResult.errorMsg("该手机号已注册");
        }
        // Preconditions.checkArgument(user == null, "该手机号已注册");  // 使用Preconditions前端收不到错误信息
        user = userService.createUser(registLoginBO);
        // 3、注册成功
        return GraceJSONResult.ok(registerOrLoginOK(user, mobile));
    }


    // 因为代码只有第二步不同，想要仅使用登录的接口：数据库存在则直接登录，不存在则注册后登录。在登录时有一个不存在的手机号就默认给它注册了。
    @PostMapping("/login")
    public GraceJSONResult login(@RequestBody @Valid RegistLoginBO registLoginBO, HttpServletRequest request) {
        String mobile = registLoginBO.getMobile();
        String redisSmsCode = stringRedisTemplate.opsForValue().get(MOBILE_SMSCODE + ":" + mobile);
        log.info("登录：inputSmsCode:{}, redisSmsCode:{}", registLoginBO.getSmsCode(), redisSmsCode);
        // 1、校验验证码
        GraceJSONResult check = checkSmsCode(registLoginBO.getSmsCode(), redisSmsCode);
        if (check != null){
            return check;
        }
        // 2、根据手机号查询用户
        Users user = userService.queryByMobile(mobile);
        if (user == null) {
            return GraceJSONResult.errorMsg("该手机号尚未注册");
        }
        // 3、登录成功
        return GraceJSONResult.ok(registerOrLoginOK(user, mobile));
    }

    /**
     * 校验验证码
     * @param inputCode 用户输入的验证码
     * @param redisSmsCode redis中的验证码
     * @return 校验结果
     */
    private GraceJSONResult checkSmsCode(String inputCode, String redisSmsCode) {
        if (StringUtils.isBlank(inputCode)) {
            return GraceJSONResult.errorMsg("验证码不能为空");
        }
        if (StringUtils.isBlank(redisSmsCode)) {
            return GraceJSONResult.errorMsg("验证码已过期");
        }
        if (!redisSmsCode.equalsIgnoreCase(inputCode)) {
            return GraceJSONResult.errorMsg("验证码错误");
        }
        return null;
    }

    /**
     * 注册/登录成功，删除redis中的验证码。
     * @param user 用户信息
     * @param mobile 手机号
     * @return 用户信息
     */
    private UsersVO registerOrLoginOK(Users user, String mobile) {
        // 3.1、注册/登录成功，删除redis中的验证码。
        stringRedisTemplate.delete(MOBILE_SMSCODE + ":" + mobile);
        // 3.2、注册/登录成功，保存用户token到redis中-拦截器会获取。 每次用一个新的 token 就可以保证多端登录，只用 id-token -token 就是唯一的
        String userToken = UUID.randomUUID().toString();
        stringRedisTemplate.opsForValue().set(REDIS_USER_TOKEN + ":" + userToken, user.getId(), 7, TimeUnit.DAYS);
        // 3.3、注册/登录成功，返回用户信息。
        UsersVO usersVo = new UsersVO();
        BeanUtils.copyProperties(user, usersVo);
        usersVo.setUserToken(userToken);
        return usersVo;
    }

    /**
     * 退出登录
     * @param userToken 用户id
     * @param request 请求
     * @return 退出登录结果
     */
    @RequestMapping("/logout")
    public GraceJSONResult logout(String userToken, HttpServletRequest request) {
        log.info("退出登录：userToken:{}", userToken);
        stringRedisTemplate.delete(REDIS_USER_TOKEN + ":" + userToken); // 多端登录进行退出指定的登录
        return GraceJSONResult.ok();
    }

}
