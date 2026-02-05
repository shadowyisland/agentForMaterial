package com.ruoyi.system.service.impl;

import cn.hutool.core.date.DateTime;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.service.ISysFileService;
import com.ruoyi.system.utils.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;

@Service
public class SysFileServiceImpl implements ISysFileService {

    /**
     * 上传文件到OSS
     */
    @Override
    public String uploadFile(MultipartFile file) {
        // 工具类获取配置
        String endpoint = FileUtils.END_POINT;
        String accessKeyId = FileUtils.KEY_ID;
        String accessKeySecret = FileUtils.KEY_SECRET;
        String bucketName = FileUtils.BUCKET_NAME;

        OSS ossClient = null;
        InputStream inputStream = null;
        try {
            // 创建OSS实例
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

            // 把文件按照日期分类，获取当前日期
            String datePath = new DateTime().toString("yyyy-MM-dd");
            // 获取上传文件的输入流
            inputStream = file.getInputStream();
            // 获取文件名称
            String originalFileName = file.getOriginalFilename();

            // 拼接日期和文件路径
            String fileName = datePath + "/" + originalFileName;

            // 判断文件是否存在
            boolean exists = ossClient.doesObjectExist(bucketName, fileName);
            if (exists) {
                // 如果文件已存在，则先删除原来的文件再进行覆盖
                ossClient.deleteObject(bucketName, fileName);
            }

            // 创建上传Object的Metadata
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentLength(inputStream.available());
            objectMetadata.setContentType(getcontentType(fileName.substring(fileName.lastIndexOf("."))));
            objectMetadata.setContentDisposition("attachment; filename=" + URLEncoder.encode(fileName, "UTF-8"));

            // 调用oss实例中的方法实现上传
            ossClient.putObject(bucketName, fileName, inputStream);

            // 把上传后文件路径返回，需要把上传到阿里云oss路径手动拼接出来
            String url = "https://" + bucketName + "." + endpoint + "/" + fileName;
            return url;
        } catch (IOException e) {
            throw new ServiceException("上传文件失败: " + e.getMessage());
        } finally {
            // 关闭输入流
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    // 静默处理关闭流时的异常，避免掩盖主流程异常
                }
            }
            // 关闭OSSClient
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    @Override
    public boolean deleteOssFileByUrl(String url) {
        if (StringUtils.isEmpty(url)) {
            return false;
        }

        // 从工具类获取配置
        String endpoint = FileUtils.END_POINT;
        String accessKeyId = FileUtils.KEY_ID;
        String accessKeySecret = FileUtils.KEY_SECRET;
        String bucketName = FileUtils.BUCKET_NAME;

        OSS ossClient = null;
        try {
            // 1. 从完整URL中提取ObjectKey
            String objectKey = extractObjectKey(url);

            if (StringUtils.isEmpty(objectKey)) {
                throw new ServiceException("无法从URL中解析文件路径");
            }

            // 2. 创建OSS实例
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

            // 3. 调用OSS客户端删除文件
            ossClient.deleteObject(bucketName, objectKey);

            return true;
        } catch (Exception e) {
            throw new ServiceException("删除文件失败: " + e.getMessage());
        } finally {
            // 关闭OSSClient
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    /**
     * 从URL中提取ObjectKey
     * 例如: https://bucket.oss-cn-shanghai.aliyuncs.com/2026-01-19/xxx.png
     * 提取为: 2026-01-19/xxx.png
     */
    private String extractObjectKey(String url) {
        try {
            // 找到第一个 "/" 的位置 (跳过 http:// 或 https://)
            int slashIndex = url.indexOf("/", 8);

            if (slashIndex > 0 && slashIndex < url.length() - 1) {
                return url.substring(slashIndex + 1);
            }
        } catch (Exception e) {
            throw new ServiceException("解析URL失败: " + e.getMessage());
        }
        return null;
    }

    /**
     * Description: 判断OSS服务文件上传时文件的contentType
     *
     * @param filenameExtension 文件后缀
     * @return String
     */
    public static String getcontentType(String filenameExtension) {
        if (filenameExtension.equalsIgnoreCase("bmp")) {
            return "image/bmp";
        }
        if (filenameExtension.equalsIgnoreCase("gif")) {
            return "image/gif";
        }
        if (filenameExtension.equalsIgnoreCase("jpeg") || filenameExtension.equalsIgnoreCase("jpg")
                || filenameExtension.equalsIgnoreCase("png")) {
            return "image/jpeg";
        }
        if (filenameExtension.equalsIgnoreCase("html")) {
            return "text/html";
        }
        if (filenameExtension.equalsIgnoreCase("txt")) {
            return "text/plain";
        }
        if (filenameExtension.equalsIgnoreCase("vsd")) {
            return "application/vnd.visio";
        }
        if (filenameExtension.equalsIgnoreCase("pptx") || filenameExtension.equalsIgnoreCase("ppt")) {
            return "application/vnd.ms-powerpoint";
        }
        if (filenameExtension.equalsIgnoreCase("docx") || filenameExtension.equalsIgnoreCase("doc")) {
            return "application/msword";
        }
        if (filenameExtension.equalsIgnoreCase("xml")) {
            return "text/xml";
        }
        return "application/octet-stream";
    }
}
