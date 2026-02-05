package com.ruoyi.system.service;

import org.springframework.web.multipart.MultipartFile;

public interface ISysFileService {
    /**
     * 上传头像到OSS
     *
     * @param file
     * @return
     */
    String uploadFile(MultipartFile file);

    /**
     * 根据完整URL删除OSS文件
     * @param url 文件完整URL
     * @return 结果
     */
    boolean deleteOssFileByUrl(String url);

}
