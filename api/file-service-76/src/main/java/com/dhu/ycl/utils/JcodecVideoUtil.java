package com.dhu.ycl.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.UUID;

// 视频操作工具类
@Slf4j
public class JcodecVideoUtil {
    // 存放截取视频某一帧的图片  "D:/docker/minio_file"
    public static String videoFramesPath = "D:/docker/minio_file/wechat/images";

    /*** 图片格式*/
    private static final String FILE_EXT = "jpg";

    /*** 帧数*/
    private static final int THUMB_FRAME = 5;

    /**
     * 获取指定视频的帧并保存为图片至指定目录
     *
     * @param videoFilePath 源视频文件路径
     * @param frameFilePath 截取帧的图片存放路径
     */
    public static void fetchFrame(String videoFilePath, String frameFilePath) throws Exception {
        File videoFile = new File(videoFilePath);
        File frameFile = new File(frameFilePath);
        getFirstFrame(videoFile, frameFile);
    }

    /**
     * 获取指定视频的帧并保存为图片至指定目录
     *
     * @param videoFile  源视频文件
     * @param targetFile 截取帧的图片
     */
    public static void fetchFrame(MultipartFile videoFile, File targetFile) throws Exception {
        File file = new File(videoFile.getOriginalFilename());
        FileUtils.copyInputStreamToFile(videoFile.getInputStream(), file);
        getFirstFrame(file, targetFile);
    }

    /**
     * 获取指定视频的帧并保存为图片至指定目录
     *
     * @param videoFile 源视频文件
     */
    public static File fetchFrame(MultipartFile videoFile) {
        String originalFilename = videoFile.getOriginalFilename();
        File file = new File(originalFilename);
        File targetFile = null;
        try {
            FileUtils.copyInputStreamToFile(videoFile.getInputStream(), file);

            int i = originalFilename.lastIndexOf(".");
            String imageName;

            if (i > 0) {
                imageName = originalFilename.substring(0, i);
            } else {
                imageName = UUID.randomUUID().toString().replace("-", "");
            }
            imageName = imageName + ".jpg";
            targetFile = new File(imageName);
            getFirstFrame(file, targetFile);
        } catch (Exception e) {
            log.error("获取视频指定帧异常：", e);
        } finally {
            if (file.exists()) {
                file.delete();
            }
        }
        log.debug("视频文件 - 帧截取 - 处理结束");
        return targetFile;
    }

    /**
     * 获取第一帧缩略图
     *
     * @param videoFile  视频路径
     * @param targetFile 缩略图目标路径
     */
    public static void getFirstFrame(File videoFile, File targetFile) {
        // FFmpegFrameGrabber 兼容性极好
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoFile)) {
            grabber.start();

            Frame frame = null;
            // 循环直到抓取到第一帧有效的视频帧（跳过可能的空头）
            for (int i = 0; i < grabber.getLengthInFrames(); ++i){
                frame = grabber.grabImage();
                // 确保抓到的是图像帧而非音频帧
                if (frame != null && frame.image != null) {
                    break;
                }
            }
            if (frame != null) {
                Java2DFrameConverter converter = new Java2DFrameConverter();
                BufferedImage bi = converter.getBufferedImage(frame);
                ImageIO.write(bi, "jpg", targetFile);
            }

            grabber.stop();
        } catch (Exception e) {
            log.error("JavaCV 截帧失败: ", e);
        }
    }

}