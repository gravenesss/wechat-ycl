package com.dhu.ycl.controller;


import com.dhu.ycl.utils.*;
import com.dhu.ycl.feign.UserInfoServiceFeign;
import com.dhu.ycl.grace.result.GraceJSONResult;
import com.dhu.ycl.grace.result.ResponseStatusEnum;
import com.dhu.ycl.pojo.vo.UsersVO;
import com.dhu.ycl.pojo.vo.VideoMsgVO;
import com.google.common.collect.Maps;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/file")
public class FileController {
    @Resource
    private MinIOConfig minIOConfig;
    @Resource
    private UserInfoServiceFeign userInfoServiceFeign;

    // 注册用户和更新用户名时会调用
    @PostMapping("/generatorQrCode")
    public String generatorQrCode(String wechatNumber, String userId) throws Exception {
        // Map 转 String 转 json 转二维码
        Map<String, String> map = Maps.newHashMap();
        map.put("wechatNumber", wechatNumber);
        map.put("userId", userId);
        String data = JsonUtils.objectToJson(map);
        // 生成二维码:TODO:查看过程
        String qrCodePath = QrCodeUtils.generateQRCode(data);

        // 把二维码上传到minio中
        if (StringUtils.isNotBlank(qrCodePath)) {
            String uuid = UUID.randomUUID().toString();
            String objectName = "wechatNumber" + MinIOUtils.SEPARATOR + userId + MinIOUtils.SEPARATOR + uuid + ".png";
            String imageQrCodeUrl = MinIOUtils.uploadFile(minIOConfig.getBucketName(), objectName, qrCodePath, true);
            return imageQrCodeUrl;
        }

        return null;
    }

    // me-设置-修改头像时调用
    @PostMapping("/uploadFace")
    public GraceJSONResult uploadFace(@RequestParam("file") MultipartFile file, String userId) {
        GraceJSONResult fileUrl = checkAndUploadFile(userId, file, "face", "image"); // 区别
        if (!fileUrl.getSuccess()) return fileUrl; // 上传失败
        String imageUrl = (String) fileUrl.getData();
        // 方式1-如下：如果前端没有保存按钮，微服务远程调用更新用户头像到数据库 OpenFeign
        // 方式2：如果前端有保存按钮，此处则不需要进行微服务调用，让前端触发保存提交到后台进行保存
        GraceJSONResult jsonResult = userInfoServiceFeign.updateFace(userId, imageUrl);         // 区别
        Object data = jsonResult.getData();
        String json = JsonUtils.objectToJson(data);
        UsersVO usersVO = JsonUtils.jsonToPojo(json, UsersVO.class);
        return GraceJSONResult.ok(usersVO);
    }

    // me-设置-修改朋友圈背景图时调用 if 下面3行；Object 下面3行，与 uploadFace 一致。
    @PostMapping("/uploadFriendCircleBg")
    public GraceJSONResult uploadFriendCircleBg(@RequestParam("file") MultipartFile file, String userId) {
        GraceJSONResult fileUrl = checkAndUploadFile(userId, file, "friendCircleBg", "image");
        if (!fileUrl.getSuccess()) return fileUrl; // 上传失败
        String imageUrl = (String) fileUrl.getData();
        // 远程调用
        GraceJSONResult jsonResult = userInfoServiceFeign.updateFriendCircleBg(userId, imageUrl);
        Object data = jsonResult.getData();
        String json = JsonUtils.objectToJson(data);
        UsersVO usersVO = JsonUtils.jsonToPojo(json, UsersVO.class);
        return GraceJSONResult.ok(usersVO);
    }

    // me-聊天背景-修改所有聊天背景图时调用
    @PostMapping("/uploadChatBg")
    public GraceJSONResult uploadChatBg(@RequestParam("file") MultipartFile file, String userId) {
        GraceJSONResult fileUrl = checkAndUploadFile(userId, file, "chatBg", "image");
        if (!fileUrl.getSuccess()) return fileUrl; // 上传失败
        String imageUrl = (String) fileUrl.getData();
        // 远程调用
        GraceJSONResult jsonResult = userInfoServiceFeign.updateChatBg(userId, imageUrl);
        Object data = jsonResult.getData();
        String json = JsonUtils.objectToJson(data);
        UsersVO usersVO = JsonUtils.jsonToPojo(json, UsersVO.class);
        return GraceJSONResult.ok(usersVO);
    }


    @PostMapping("uploadFriendCircleImage")
    public GraceJSONResult uploadFriendCircleImage(@RequestParam("file") MultipartFile file, String userId) {
        return checkAndUploadFile(userId, file, "friendCircleImage", "image");
    }

    @PostMapping("uploadChatPhoto")
    public GraceJSONResult uploadChatPhoto(@RequestParam("file") MultipartFile file, String userId) {
        return checkAndUploadFile(userId, file, "chat", "image");
    }

    @PostMapping("uploadChatVoice")
    public GraceJSONResult uploadChatVoice(@RequestParam("file") MultipartFile file, String userId) {
        return checkAndUploadFile(userId, file, "chat", "voice");
    }

    @PostMapping("uploadChatVideo")
    public GraceJSONResult uploadChatVideo(@RequestParam("file") MultipartFile file, String userId) throws Exception {
        GraceJSONResult fileUrl = checkAndUploadFile(userId, file, "chat", "video");
        if (!fileUrl.getSuccess()) return fileUrl; // 上传失败
        // 上传视频成功，则获取视频的url
        String videoUrl = (String) fileUrl.getData();

        // 帧，封面获取 = 视频截帧 截取第一帧
        String coverName = UUID.randomUUID() + ".jpg";   // 视频封面的名称
        String coverPath = JcodecVideoUtil.videoFramesPath + MinIOUtils.SEPARATOR + "videos" + MinIOUtils.SEPARATOR + coverName;
        File coverFile = new File(coverPath);
        if (!coverFile.getParentFile().exists()) {
            coverFile.getParentFile().mkdirs();
        }
        JcodecVideoUtil.fetchFrame(file, coverFile);
        // 上传封面到minio
        String coverUrl = MinIOUtils.uploadFile(minIOConfig.getBucketName(), coverName, new FileInputStream(coverFile), true);

        VideoMsgVO videoMsgVO = new VideoMsgVO();
        videoMsgVO.setVideoPath(videoUrl);
        videoMsgVO.setCover(coverUrl);

        return GraceJSONResult.ok(videoMsgVO);
    }

    /**
     * 检查userId和文件，并返回url
     *
     * @param userId   用户id
     * @param file     文件
     * @param prefix   文件前缀
     * @param fileType 文件类型
     * @return 文件名url
     */
    private GraceJSONResult checkAndUploadFile(String userId, MultipartFile file, String prefix, String fileType) {
        if (StringUtils.isBlank(userId)) {
            return GraceJSONResult.errorCustom(ResponseStatusEnum.FILE_UPLOAD_FAILD);
        }
        String filename = file.getOriginalFilename();   // 获得文件原始名称
        if (StringUtils.isBlank(filename)) {
            return GraceJSONResult.errorCustom(ResponseStatusEnum.FILE_UPLOAD_FAILD);
        }
        // 重命名文件
        String fileSuffix = filename.substring(filename.lastIndexOf("."));
        String uuid = UUID.randomUUID().toString();
        filename = prefix + MinIOUtils.SEPARATOR + userId + MinIOUtils.SEPARATOR + fileType + MinIOUtils.SEPARATOR + uuid + fileSuffix;
        String fileUrl;
        try {
            fileUrl = MinIOUtils.uploadFile(minIOConfig.getBucketName(), filename, file.getInputStream(), true);
        } catch (Exception e) {
            return GraceJSONResult.errorCustom(ResponseStatusEnum.FILE_UPLOAD_FAILD);
        }
        return GraceJSONResult.ok(fileUrl);
    }
}
// TODO：历史图片的删除。。