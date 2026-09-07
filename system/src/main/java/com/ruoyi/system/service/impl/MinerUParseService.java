package com.ruoyi.system.service.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

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

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;

/**
 * MinerU 文档解析服务。
 */
@Service
public class MinerUParseService {
    private static final Logger log = LoggerFactory.getLogger(MinerUParseService.class);

    @Value("${file.parse.api.url:http://localhost:8000}")
    private String fileParseApiUrl;

    @Value("${file.parse.api.connectTimeout:30000}")
    private int connectTimeout;

    @Value("${file.parse.api.responseTimeout:300000}")
    private int responseTimeout;

    public String parseToMarkdown(String localFilePath, String originalFileName) {
        File file = new File(localFilePath);
        if (!file.exists() || !file.isFile()) {
            throw new ServiceException("文件不存在: " + localFilePath);
        }

        String requestFileName = buildRequestFileName(originalFileName, file);
        String url = buildFileParseUrl();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(connectTimeout))
                .setResponseTimeout(Timeout.ofMilliseconds(responseTimeout))
                .build();

        try (CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(requestConfig).build();
             InputStream fileStream = new FileInputStream(file)) {
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setEntity(buildMultipartEntity(fileStream, requestFileName));

            log.info("开始调用 MinerU 解析接口: {}, fileName: {}", url, requestFileName);
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                HttpEntity responseEntity = response.getEntity();
                String responseBody = responseEntity == null ? "" : EntityUtils.toString(responseEntity, "UTF-8");
                int statusCode = response.getCode();
                if (statusCode != 200) {
                    log.error("MinerU 解析接口调用失败，状态码: {}, 响应: {}", statusCode, responseBody);
                    throw new ServiceException("MinerU 解析接口调用失败: HTTP " + statusCode);
                }
                return getMarkdownContent(responseBody, requestFileName);
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用 MinerU 解析接口异常", e);
            throw new ServiceException("调用 MinerU 解析接口失败: " + e.getMessage());
        }
    }

    private HttpEntity buildMultipartEntity(InputStream fileStream, String fileName) {
        return MultipartEntityBuilder.create()
                .addBinaryBody("files", fileStream, ContentType.APPLICATION_OCTET_STREAM, fileName)
                .addTextBody("backend", "pipeline")
                .addTextBody("response_format_zip", "false")
                .addTextBody("return_images", "false")
                .addTextBody("return_model_output", "false")
                .addTextBody("return_middle_json", "false")
                .build();
    }

    private String getMarkdownContent(String responseBody, String fileName) {
        JSONObject jsonObject = JSON.parseObject(responseBody);
        JSONObject results = jsonObject.getJSONObject("results");
        if (results == null || results.isEmpty()) {
            throw new ServiceException("MinerU 返回结果缺少 results");
        }

        JSONObject fileResult = results.getJSONObject(fileName);
        if (fileResult == null && results.size() == 1) {
            fileResult = results.getJSONObject(results.keySet().iterator().next());
        }
        if (fileResult == null) {
            throw new ServiceException("MinerU 返回结果缺少文件解析内容");
        }

        String markdownContent = fileResult.getString("md_content");
        if (markdownContent == null) {
            throw new ServiceException("MinerU 返回结果缺少 md_content");
        }
        return markdownContent;
    }

    private String buildFileParseUrl() {
        if (fileParseApiUrl.endsWith("/")) {
            return fileParseApiUrl.substring(0, fileParseApiUrl.length() - 1) + "/file_parse";
        }
        return fileParseApiUrl + "/file_parse";
    }

    private String buildRequestFileName(String originalFileName, File file) {
        String fileName = StringUtils.isNotEmpty(originalFileName) ? originalFileName : file.getName();
        String suffix = "";
        int dotIndex = fileName.lastIndexOf(".");
        if (dotIndex > -1) {
            suffix = fileName.substring(dotIndex);
        }
        return "document_" + Math.abs(file.getAbsolutePath().hashCode()) + suffix;
    }
}
