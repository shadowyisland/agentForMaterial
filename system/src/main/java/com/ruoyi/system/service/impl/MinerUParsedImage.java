package com.ruoyi.system.service.impl;

/** MinerU ZIP 中解出的图片二进制内容。 */
public class MinerUParsedImage
{
    private final String name;
    private final String contentType;
    private final byte[] content;

    public MinerUParsedImage(String name, String contentType, byte[] content)
    {
        this.name = name;
        this.contentType = contentType;
        this.content = content;
    }

    public String getName()
    {
        return name;
    }

    public String getContentType()
    {
        return contentType;
    }

    public byte[] getContent()
    {
        return content;
    }
}
