package com.ruoyi.system.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;
import org.springframework.core.io.Resource;

import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.SysDocument;

/** 将上传文件按页转为图片，只读预览不暴露 PDF 表单和编辑功能。 */
@Service
public class DocumentPreviewService
{
    public Map<String, Object> getTemplateInfo(Resource preview) throws IOException
    {
        try (InputStream input = preview.getInputStream(); PDDocument source = PDDocument.load(input))
        {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("pageCount", source.getNumberOfPages());
            info.put("type", "pdf");
            return info;
        }
    }

    public void writeTemplatePage(Resource preview, int page, OutputStream output) throws IOException
    {
        try (InputStream input = preview.getInputStream(); PDDocument source = PDDocument.load(input))
        {
            ImageIO.write(renderPage(source, page), "png", output);
        }
    }

    public Map<String, Object> getUploadInfo(SysDocument document) throws IOException
    {
        File file = resolveUpload(document);
        boolean pdf = isPdf(file);
        int pageCount = 1;
        if (pdf)
        {
            try (PDDocument source = PDDocument.load(file))
            {
                pageCount = source.getNumberOfPages();
            }
        }
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("pageCount", pageCount);
        info.put("type", pdf ? "pdf" : "image");
        return info;
    }

    public void writeUploadPage(SysDocument document, int page, OutputStream output) throws IOException
    {
        File file = resolveUpload(document);
        BufferedImage image;
        if (isPdf(file))
        {
            try (PDDocument source = PDDocument.load(file))
            {
                image = renderPage(source, page);
            }
        }
        else
        {
            validatePage(page, 1);
            image = ImageIO.read(file);
        }
        if (image == null)
        {
            throw new ServiceException("无法预览此图片");
        }
        ImageIO.write(image, "png", output);
    }

    private BufferedImage renderPage(PDDocument source, int page) throws IOException
    {
        validatePage(page, source.getNumberOfPages());
        return new PDFRenderer(source).renderImageWithDPI(page - 1, 144, ImageType.RGB);
    }

    private void validatePage(int page, int total)
    {
        if (page < 1 || page > total)
        {
            throw new ServiceException("预览页码超出范围");
        }
    }

    private boolean isPdf(File file)
    {
        return file.getName().toLowerCase(Locale.ROOT).endsWith(".pdf");
    }

    private File resolveUpload(SysDocument document) throws IOException
    {
        String path = document.getFilePath();
        String prefix = Constants.RESOURCE_PREFIX + "/";
        if (path == null || !path.startsWith(prefix))
        {
            throw new ServiceException("上传文件路径不正确");
        }
        File root = new File(RuoYiConfig.getProfile()).getCanonicalFile();
        File file = new File(root, path.substring(prefix.length())).getCanonicalFile();
        if (!file.toPath().startsWith(root.toPath()) || !file.isFile())
        {
            throw new ServiceException("上传文件不存在");
        }
        if (!file.getName().toLowerCase(Locale.ROOT).matches(".+\\.(pdf|png|jpg|jpeg)"))
        {
            throw new ServiceException("仅支持预览 PDF、PNG 和 JPG 文件");
        }
        return file;
    }
}
