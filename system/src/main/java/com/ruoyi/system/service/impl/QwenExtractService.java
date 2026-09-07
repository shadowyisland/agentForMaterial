package com.ruoyi.system.service.impl;

import java.nio.charset.StandardCharsets;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 远程 llama.cpp OpenAI-compatible 抽取客户端。
 */
@Service
public class QwenExtractService
{
    private static final Logger log = LoggerFactory.getLogger(QwenExtractService.class);

    @Value("${llm.qwen.base-url:http://127.0.0.1:8081/v1}")
    private String baseUrl;

    @Value("${llm.qwen.model:qwen3.8-27b}")
    private String modelName;

    @Value("${llm.qwen.connect-timeout:30000}")
    private int connectTimeout;

    @Value("${llm.qwen.response-timeout:300000}")
    private int responseTimeout;

    @Value("${llm.qwen.temperature:0}")
    private int temperature;

    @Value("${llm.qwen.max-tokens:4096}")
    private int maxTokens;

    public String getModelName()
    {
        return modelName;
    }

    public String extractJson(String systemPrompt, String ocrContent)
    {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(connectTimeout))
                .setResponseTimeout(Timeout.ofMilliseconds(responseTimeout))
                .build();
        HttpPost request = new HttpPost(buildChatUrl());
        request.setHeader("Accept", "application/json");
        request.setEntity(new StringEntity(buildRequestBody(systemPrompt, ocrContent).toJSONString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpClient client = HttpClients.custom().setDefaultRequestConfig(requestConfig).build();
             CloseableHttpResponse response = client.execute(request))
        {
            HttpEntity entity = response.getEntity();
            String body = entity == null ? "" : EntityUtils.toString(entity, StandardCharsets.UTF_8);
            if (response.getCode() != 200)
            {
                log.error("Qwen 调用失败，状态码: {}, 响应: {}", response.getCode(), body);
                throw new ServiceException("大模型调用失败: HTTP " + response.getCode());
            }
            return parseContent(body);
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            log.error("调用 Qwen 抽取异常", e);
            throw new ServiceException("调用大模型失败: " + e.getMessage());
        }
    }

    private JSONObject buildRequestBody(String systemPrompt, String ocrContent)
    {
        JSONObject request = new JSONObject();
        request.put("model", modelName);
        JSONArray messages = new JSONArray();
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        messages.add(systemMessage);
        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", "OCR 文本如下：\n" + ocrContent);
        messages.add(userMessage);
        request.put("messages", messages);
        request.put("temperature", temperature);
        request.put("max_tokens", maxTokens);
        JSONObject thinking = new JSONObject();
        thinking.put("enable_thinking", false);
        request.put("chat_template_kwargs", thinking);
        JSONObject responseFormat = new JSONObject();
        responseFormat.put("type", "json_object");
        request.put("response_format", responseFormat);
        return request;
    }

    private String parseContent(String body)
    {
        JSONObject response = JSON.parseObject(body);
        JSONArray choices = response.getJSONArray("choices");
        if (choices == null || choices.isEmpty())
        {
            throw new ServiceException("大模型返回内容为空");
        }
        JSONObject message = choices.getJSONObject(0).getJSONObject("message");
        String content = message == null ? null : message.getString("content");
        if (StringUtils.isEmpty(content))
        {
            throw new ServiceException("大模型未返回 JSON 内容");
        }
        String jsonContent = extractJsonObject(content);
        try
        {
            JSONObject json = JSON.parseObject(jsonContent);
            if (json == null)
            {
                throw new ServiceException("大模型返回的不是 JSON 对象");
            }
            return JSON.toJSONString(json);
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ServiceException("大模型返回的 JSON 格式不正确");
        }
    }

    private String extractJsonObject(String content)
    {
        String trimmed = content.trim();
        if (trimmed.startsWith("```"))
        {
            int firstLineEnd = trimmed.indexOf('\n');
            trimmed = firstLineEnd >= 0 ? trimmed.substring(firstLineEnd + 1) : trimmed;
            int codeFence = trimmed.lastIndexOf("```");
            if (codeFence >= 0)
            {
                trimmed = trimmed.substring(0, codeFence);
            }
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end <= start)
        {
            throw new ServiceException("大模型返回内容中未找到 JSON 对象");
        }
        return trimmed.substring(start, end + 1);
    }

    private String buildChatUrl()
    {
        String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return normalizedBase + "/chat/completions";
    }
}
