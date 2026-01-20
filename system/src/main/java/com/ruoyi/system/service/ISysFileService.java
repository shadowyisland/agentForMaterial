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
}
