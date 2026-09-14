package com.ruoyi.system.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;

/** MinerU 文档解析服务，使用 ZIP 响应同时获取 Markdown 与图片。 */
@Service
public class MinerUParseService
{
    private static final Logger log = LoggerFactory.getLogger(MinerUParseService.class);

    @Value("${file.parse.api.url:http://localhost:8000}")
    private String fileParseApiUrl;

    @Value("${file.parse.api.connectTimeout:30000}")
    private int connectTimeout;

    @Value("${file.parse.api.responseTimeout:300000}")
    private int responseTimeout;

    public MinerUParseResult parse(String localFilePath, String originalFileName)
    {
        File file = new File(localFilePath);
        if (!file.exists() || !file.isFile())
        {
            throw new ServiceException("文件不存在: " + localFilePath);
        }

        String requestFileName = buildRequestFileName(originalFileName, file);
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(connectTimeout))
                .setResponseTimeout(Timeout.ofMilliseconds(responseTimeout))
                .build();

        try (CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(requestConfig).build();
                InputStream fileStream = new FileInputStream(file))
        {
            HttpPost httpPost = new HttpPost(buildFileParseUrl());
            httpPost.setHeader("Accept", "application/zip");
            httpPost.setEntity(buildMultipartEntity(fileStream, requestFileName));

            log.info("开始调用 MinerU ZIP 解析接口: {}, fileName: {}", httpPost.getUri(), requestFileName);
            try (CloseableHttpResponse response = httpClient.execute(httpPost))
            {
                HttpEntity responseEntity = response.getEntity();
                int statusCode = response.getCode();
                if (statusCode != 200 || responseEntity == null)
                {
                    String responseBody = responseEntity == null ? "" : EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);
                    log.error("MinerU 解析接口调用失败，状态码: {}, 响应: {}", statusCode, responseBody);
                    throw new ServiceException("MinerU 解析接口调用失败: HTTP " + statusCode);
                }
                return readZip(EntityUtils.toByteArray(responseEntity));
            }
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            log.error("调用 MinerU 解析接口异常", e);
            throw new ServiceException("调用 MinerU 解析接口失败: " + e.getMessage());
        }
    }

    private HttpEntity buildMultipartEntity(InputStream fileStream, String fileName)
    {
        return MultipartEntityBuilder.create()
                .addBinaryBody("files", fileStream, ContentType.APPLICATION_OCTET_STREAM, fileName)
                .addTextBody("backend", "pipeline")
                .addTextBody("response_format_zip", "true")
                .addTextBody("return_images", "true")
                .addTextBody("return_model_output", "false")
                .addTextBody("return_middle_json", "false")
                .build();
    }

    private MinerUParseResult readZip(byte[] zipContent)
    {
        String markdown = null;
        List<MinerUParsedImage> images = new ArrayList<MinerUParsedImage>();
        try (ZipInputStream zip = new ZipInputStream(new java.io.ByteArrayInputStream(zipContent)))
        {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null)
            {
                if (entry.isDirectory())
                {
                    continue;
                }
                String entryName = entry.getName();
                byte[] content = readEntry(zip);
                if (entryName.toLowerCase().endsWith(".md"))
                {
                    markdown = new String(content, StandardCharsets.UTF_8);
                }
                else if (isImage(entryName))
                {
                    images.add(new MinerUParsedImage(fileName(entryName), contentType(entryName), content));
                }
            }
        }
        catch (Exception e)
        {
            throw new ServiceException("MinerU 返回内容不是有效 ZIP: " + e.getMessage());
        }
        if (markdown == null)
        {
            throw new ServiceException("MinerU ZIP 中缺少 Markdown 文件");
        }
        return new MinerUParseResult(markdown, images);
    }

    private byte[] readEntry(InputStream inputStream) throws Exception
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int length;
        while ((length = inputStream.read(buffer)) != -1)
        {
            output.write(buffer, 0, length);
        }
        return output.toByteArray();
    }

    private boolean isImage(String name)
    {
        String lower = name.toLowerCase();
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".gif") || lower.endsWith(".bmp");
    }

    private String contentType(String name)
    {
        String lower = name.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".bmp")) return "image/bmp";
        return "image/jpeg";
    }

    private String fileName(String name)
    {
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        return slash < 0 ? name : name.substring(slash + 1);
    }

    private String buildFileParseUrl()
    {
        return fileParseApiUrl.endsWith("/")
                ? fileParseApiUrl.substring(0, fileParseApiUrl.length() - 1) + "/file_parse"
                : fileParseApiUrl + "/file_parse";
    }

    private String buildRequestFileName(String originalFileName, File file)
    {
        String fileName = StringUtils.isNotEmpty(originalFileName) ? originalFileName : file.getName();
        String suffix = "";
        int dotIndex = fileName.lastIndexOf(".");
        if (dotIndex > -1)
        {
            suffix = fileName.substring(dotIndex);
        }
        return "document_" + Math.abs(file.getAbsolutePath().hashCode()) + suffix;
    }
}
