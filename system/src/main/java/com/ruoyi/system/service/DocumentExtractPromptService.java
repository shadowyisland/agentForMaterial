package com.ruoyi.system.service;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import com.ruoyi.common.exception.ServiceException;

/**
 * 从 resources/prompts 读取文档抽取提示词。
 */
@Service
public class DocumentExtractPromptService
{
    private static final ZoneId CHINA_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private final Map<DocumentExtractPrompt, String> promptCache = new ConcurrentHashMap<DocumentExtractPrompt, String>();
    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    public String getPrompt(String materialCategory, String documentKind)
    {
        DocumentExtractPrompt prompt = DocumentExtractPrompt.find(materialCategory, documentKind);
        if (prompt == null)
        {
            throw new ServiceException("当前材料分类未配置 AI 抽取提示词");
        }
        String template = promptCache.get(prompt);
        if (template == null)
        {
            template = loadPrompt(prompt);
            promptCache.put(prompt, template);
        }
        if (template.contains("TODO"))
        {
            throw new ServiceException("当前 " + materialCategory + " " + documentKind + " 提示词尚未配置");
        }
        // OCR 正文在 OpenAI-compatible 请求的 user 消息中单独传递。
        return template.replace("{{today}}", LocalDate.now(CHINA_ZONE).format(DATE_FORMAT))
                .replace("{{ocrContent}}", "")
                .replaceAll("(?s)\\s*OCR 文本如下：\\s*$", "");
    }

    private String loadPrompt(DocumentExtractPrompt prompt)
    {
        try
        {
            Resource resource = resolver.getResource("classpath:prompts/" + prompt.getFileName());
            if (!resource.exists())
            {
                throw new ServiceException("提示词文件不存在: " + prompt.getFileName());
            }
            return FileCopyUtils.copyToString(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ServiceException("读取提示词失败: " + e.getMessage());
        }
    }
}
