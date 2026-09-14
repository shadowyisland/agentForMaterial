package com.ruoyi.system.service.impl;

import java.util.Collections;
import java.util.List;

/** MinerU ZIP 解析出的 Markdown 与图片。 */
public class MinerUParseResult
{
    private final String markdownContent;
    private final List<MinerUParsedImage> images;

    public MinerUParseResult(String markdownContent, List<MinerUParsedImage> images)
    {
        this.markdownContent = markdownContent;
        this.images = images == null ? Collections.<MinerUParsedImage>emptyList() : images;
    }

    public String getMarkdownContent()
    {
        return markdownContent;
    }

    public List<MinerUParsedImage> getImages()
    {
        return images;
    }
}
