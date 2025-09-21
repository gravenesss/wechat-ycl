package com.dhu.ycl.controller;


import com.dhu.ycl.base.BaseInfoProperties;
import com.dhu.ycl.grace.result.GraceJSONResult;
import com.dhu.ycl.grace.result.ResponseStatusEnum;
import com.dhu.ycl.pojo.Users;
import com.dhu.ycl.pojo.bo.ModifyUserBO;
import com.dhu.ycl.pojo.vo.UsersVO;
import com.dhu.ycl.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/userinfo")
public class UserController extends BaseInfoProperties {
    @Resource
    private UserService userService;

    // APP.vue 即主页面；friendPage、singleChatPage、msgListPage、verifyFriend
    @PostMapping("/get")
    public GraceJSONResult get(@RequestParam("userId") String userId) {
        return GraceJSONResult.ok(this.getUserInfo(userId, false));
    }


    // me/userInfo个人信息界面 普通信息的修改。
    @PostMapping("/modify")
    public GraceJSONResult modify(@RequestBody ModifyUserBO userBO) {
        return UpdateAndGetLatestUser(userBO, true);
    }

    @PostMapping("/updateFace")
    public GraceJSONResult updateFace(@RequestParam("userId") String userId, @RequestParam("face") String face) {
        ModifyUserBO userBO = new ModifyUserBO();
        userBO.setUserId(userId);
        userBO.setFace(face);
        return UpdateAndGetLatestUser(userBO, true);
    }

    @PostMapping("/updateFriendCircleBg")
    public GraceJSONResult updateFriendCircleBg(@RequestParam("userId") String userId,
                                                @RequestParam("friendCircleBg") String friendCircleBg) {
        ModifyUserBO userBO = new ModifyUserBO();
        userBO.setUserId(userId);
        userBO.setFriendCircleBg(friendCircleBg);
        return UpdateAndGetLatestUser(userBO, true);
    }

    @PostMapping("/updateChatBg")
    public GraceJSONResult updateChatBg(@RequestParam("userId") String userId, @RequestParam("chatBg") String chatBg) {
        ModifyUserBO userBO = new ModifyUserBO();
        userBO.setUserId(userId);
        userBO.setChatBg(chatBg);
        return UpdateAndGetLatestUser(userBO, true);
    }

    /**
     * 修改用户信息并返回最新用户信息
     *
     * @param userBO    修改用户信息BO
     * @param needToken 是否需要返回token
     * @return 最新用户信息
     */
    private GraceJSONResult UpdateAndGetLatestUser(ModifyUserBO userBO, boolean needToken) {
        userService.modifyUserInfo(userBO);  // 修改用户信息
        return GraceJSONResult.ok(this.getUserInfo(userBO.getUserId(), needToken));  // 返回最新用户信息
    }

    /**
     * 返回最新用户信息
     *
     * @param userId    用户id
     * @param needToken 是否需要返回token
     * @return 最新用户信息
     */
    private UsersVO getUserInfo(String userId, boolean needToken) {
        // 查询获得用户的最新信息
        Users latestUser = userService.getById(userId);
        UsersVO usersVO = new UsersVO();
        BeanUtils.copyProperties(latestUser, usersVO);

        if (needToken) { // token对userId，多设备可同时登录
            String uToken = TOKEN_USER_PREFIX + SYMBOL_DOT + UUID.randomUUID(); // app.uuid
            redis.setByDays(REDIS_USER_TOKEN + ":" + uToken, userId, 7);   // redis_user_token:app.uuid  -- userId
            usersVO.setUserToken(uToken);
        }
        return usersVO;
    }


    // 搜索好友后展示：根据微信号或手机号查询
    @PostMapping("/queryFriend")
    public GraceJSONResult queryFriend(String queryString, HttpServletRequest request) {
        if (StringUtils.isBlank(queryString)) {
            return GraceJSONResult.error();
        }
        Users friend = userService.getByWechatNumOrMobile(queryString);
        if (friend == null) {
            return GraceJSONResult.errorCustom(ResponseStatusEnum.FRIEND_NOT_EXIST_ERROR);
        }
        // 判断，不能添加自己为好友
        String myId = request.getHeader(HEADER_USER_ID);
        if (myId.equals(friend.getId())) {
            return GraceJSONResult.errorCustom(ResponseStatusEnum.CAN_NOT_ADD_SELF_FRIEND_ERROR);
        }
        return GraceJSONResult.ok(friend);
    }
}
