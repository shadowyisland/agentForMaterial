package com.ruoyi.system.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.DocumentMineruImage;
import com.ruoyi.system.domain.SysDocument;
import com.ruoyi.system.mapper.SysDocumentMapper;
import com.ruoyi.system.service.impl.MinerUParsedImage;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 管理当前文档 MinerU 图片在 MinIO 中的存储与读取。
 * 数据库仅保存对象键和图片元数据，前端与 Word 导出均通过图片 ID 读取。
 */
@Service
public class DocumentMineruImageService
{
    @Autowired
    private MinioClient minioClient;

    @Autowired
    private SysDocumentMapper documentMapper;

    @Value("${minio.bucket-name}")
    private String bucketName;

    public List<DocumentMineruImage> replaceImages(SysDocument document, List<MinerUParsedImage> parsedImages)
    {
        List<DocumentMineruImage> oldImages = listImages(document);
        List<DocumentMineruImage> uploaded = new ArrayList<DocumentMineruImage>();
        try
        {
            ensureBucket();
            for (MinerUParsedImage image : parsedImages)
            {
                if (image == null || image.getContent() == null || image.getContent().length == 0)
                {
                    continue;
                }
                DocumentMineruImage stored = new DocumentMineruImage();
                stored.setImageId(UUID.randomUUID().toString());
                stored.setName(safeFileName(image.getName()));
                stored.setContentType(image.getContentType());
                stored.setSize((long) image.getContent().length);
                stored.setObjectKey("mineru/document/" + document.getDocumentId() + "/"
                        + stored.getImageId() + "-" + stored.getName());
                putObject(stored, image.getContent());
                uploaded.add(stored);
            }
        }
        catch (Exception e)
        {
            removeObjects(uploaded);
            throw new ServiceException("保存 MinerU 解析图片失败: " + e.getMessage());
        }

        document.setMineruImages(JSON.toJSONString(uploaded));
        documentMapper.updateDocument(document);
        removeObjects(oldImages);
        return uploaded;
    }

    public List<DocumentMineruImage> listImages(SysDocument document)
    {
        if (document == null || StringUtils.isEmpty(document.getMineruImages()))
        {
            return Collections.emptyList();
        }
        try
        {
            List<DocumentMineruImage> images = JSON.parseArray(document.getMineruImages(), DocumentMineruImage.class);
            return images == null ? Collections.<DocumentMineruImage>emptyList() : images;
        }
        catch (Exception e)
        {
            throw new ServiceException("MinerU 图片元数据格式不正确");
        }
    }

    public DocumentMineruImage getImage(SysDocument document, String imageId)
    {
        for (DocumentMineruImage image : listImages(document))
        {
            if (StringUtils.equals(imageId, image.getImageId()))
            {
                return image;
            }
        }
        throw new ServiceException("当前上传文件中不存在该图片");
    }

    public InputStream openImage(SysDocument document, String imageId)
    {
        DocumentMineruImage image = getImage(document, imageId);
        try
        {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(image.getObjectKey())
                    .build());
        }
        catch (Exception e)
        {
            throw new ServiceException("读取 MinerU 图片失败: " + e.getMessage());
        }
    }

    public byte[] readImage(SysDocument document, String imageId)
    {
        try (InputStream inputStream = openImage(document, imageId))
        {
            return readAllBytes(inputStream);
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ServiceException("读取 MinerU 图片失败: " + e.getMessage());
        }
    }

    public void deleteImages(SysDocument document)
    {
        // 删除文档必须确认 MinIO 对象已清理；失败则让删除接口失败，用户可重试，
        // 不能静默留下无主图片。
        for (DocumentMineruImage image : listImages(document))
        {
            if (image == null || StringUtils.isEmpty(image.getObjectKey()))
            {
                continue;
            }
            try
            {
                minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(image.getObjectKey())
                        .build());
            }
            catch (Exception e)
            {
                throw new ServiceException("删除 MinerU 图片失败: " + e.getMessage());
            }
        }
    }

    private void ensureBucket() throws Exception
    {
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build()))
        {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
    }

    private void putObject(DocumentMineruImage image, byte[] content) throws Exception
    {
        try (InputStream inputStream = new ByteArrayInputStream(content))
        {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(image.getObjectKey())
                    .stream(inputStream, content.length, -1)
                    .contentType(image.getContentType())
                    .build());
        }
    }

    private void removeObjects(List<DocumentMineruImage> images)
    {
        for (DocumentMineruImage image : images)
        {
            try
            {
                if (image != null && StringUtils.isNotEmpty(image.getObjectKey()))
                {
                    minioClient.removeObject(RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(image.getObjectKey())
                            .build());
                }
            }
            catch (Exception ignored)
            {
                // 图片对象清理失败不应影响已完成的 OCR 或文档删除。
            }
        }
    }

    private String safeFileName(String fileName)
    {
        String value = StringUtils.isEmpty(fileName) ? "image.png" : fileName;
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private byte[] readAllBytes(InputStream inputStream) throws Exception
    {
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int length;
        while ((length = inputStream.read(buffer)) != -1)
        {
            output.write(buffer, 0, length);
        }
        return output.toByteArray();
    }
}
