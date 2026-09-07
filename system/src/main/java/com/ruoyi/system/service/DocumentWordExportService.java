package com.ruoyi.system.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import javax.imageio.ImageIO;

import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.SysDocument;

/** 使用填写后的 JSON 回填原 Word 模板，格式和字段位置由模板决定。 */
@Service
public class DocumentWordExportService
{
    private static final String STRUCTURE_IMAGE = "{{@分子结构}}";

    @Autowired
    private DocumentTemplateResolver documentTemplateResolver;

    public void writeDocument(SysDocument document, String finalJson, OutputStream outputStream)
    {
        JSONObject root = parseJson(finalJson);
        Resource template = documentTemplateResolver.resolve(document.getMaterialCategory(), document.getDocumentKind());
        root.put("文档名称", StringUtils.defaultString(document.getDocumentName()));
        root.putIfAbsent("产品名称", StringUtils.defaultString(document.getProductName()));
        root.put("产品型号", StringUtils.defaultString(document.getProductModel()));
        root.put("内部编号", StringUtils.defaultString(document.getInternalCode()));
        normalizeFillerSolventRows(document, root);
        try (InputStream input = template.getInputStream())
        {
            DocumentWordTemplateRenderer renderer = new DocumentWordTemplateRenderer();
            JSONArray images = root.getJSONArray("图片");
            if ("EPOXY".equals(document.getMaterialCategory()) && "TDS".equals(document.getDocumentKind())
                    && images != null && !images.isEmpty())
            {
                // 图片先写入专用标记所在段落，再渲染文字，避免按标题插入时改变位置。
                try (XWPFDocument word = new XWPFDocument(input))
                {
                    insertStructureImage(word, images);
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    word.write(buffer);
                    renderer.render(new ByteArrayInputStream(buffer.toByteArray()), root, outputStream);
                }
            }
            else
            {
                renderer.render(input, root, outputStream);
            }
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ServiceException("生成 Word 文件失败: " + e.getMessage());
        }
    }

    public void validateTemplate(SysDocument document)
    {
        documentTemplateResolver.resolve(document.getMaterialCategory(), document.getDocumentKind());
    }

    private void normalizeFillerSolventRows(SysDocument document, JSONObject root)
    {
        if (!"FILLER".equals(document.getMaterialCategory()) || !"TDS".equals(document.getDocumentKind()))
        {
            return;
        }
        JSONObject specifications = root.getJSONObject("物化参数");
        if (specifications == null)
        {
            return;
        }
        JSONArray solvents = specifications.getJSONArray("溶剂/分散剂");
        if (solvents == null)
        {
            return;
        }
        for (int i = 0; i < solvents.size(); i++)
        {
            JSONObject row = solvents.getJSONObject(i);
            if (row != null && "种类".equals(row.getString("类别")))
            {
                // 模板最后一格原有 {{.输出}} 标记，种类名填入该格；含量行仍使用 {{.数值}}。
                row.put("输出", StringUtils.defaultString(row.getString("种类名")));
            }
        }
    }

    private JSONObject parseJson(String finalJson)
    {
        try
        {
            JSONObject root = JSON.parseObject(finalJson);
            if (root == null)
            {
                throw new ServiceException("最终 JSON 不能为空");
            }
            return root;
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ServiceException("最终 JSON 格式不正确");
        }
    }

    private void insertStructureImage(XWPFDocument word, JSONArray images)
    {
        for (int i = 0; i < images.size(); i++)
        {
            JSONObject image = images.getJSONObject(i);
            if (image == null || StringUtils.isEmpty(image.getString("路径"))
                    || !"分子结构后".equals(image.getString("位置")))
            {
                continue;
            }
            for (XWPFParagraph paragraph : word.getParagraphs())
            {
                if (STRUCTURE_IMAGE.equals(paragraph.getText().trim()))
                {
                    addPicture(word, paragraph, image.getString("路径"), image.getString("名称"));
                    return;
                }
            }
            throw new ServiceException("Word 模板缺少分子结构图片标记");
        }
    }

    private void addPicture(XWPFDocument word, XWPFParagraph paragraph, String path, String pictureName)
    {
        File imageFile = resolveImageFile(path);
        if (!imageFile.isFile())
        {
            throw new ServiceException("图片文件不存在: " + path);
        }
        try (InputStream inputStream = new FileInputStream(imageFile))
        {
            BufferedImage image = ImageIO.read(imageFile);
            if (image == null)
            {
                throw new ServiceException("图片格式不正确: " + imageFile.getName());
            }
            Node section = word.getDocument().getBody().getSectPr().getDomNode();
            double availableWidth = sectionValue(section, "pgSz", "w", 12240)
                    - sectionValue(section, "pgMar", "left", 1440) - sectionValue(section, "pgMar", "right", 1440);
            double columns = sectionValue(section, "cols", "num", 1);
            double gap = sectionValue(section, "cols", "space", 0);
            // Units.toEMU 使用磅，1 磅 = 20 twip；图片还需扣除段落左右缩进。
            double widthLimit = ((availableWidth - (columns - 1) * gap) / columns
                    - Math.max(0, paragraph.getIndentationLeft()) - Math.max(0, paragraph.getIndentationRight())) / 20.0;
            double scale = Math.min(widthLimit / image.getWidth(), 270.0 / image.getHeight());
            double width = image.getWidth() * scale;
            double height = image.getHeight() * scale;
            XWPFRun run = paragraph.createRun();
            run.addPicture(inputStream, getPictureType(imageFile.getName()),
                    StringUtils.isEmpty(pictureName) ? imageFile.getName() : pictureName,
                    Units.toEMU(width), Units.toEMU(height));
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ServiceException("插入图片失败: " + e.getMessage());
        }
    }

    private double sectionValue(Node section, String name, String attribute, double defaultValue)
    {
        // 直接读取 OOXML，避免为页面属性额外引入完整的 POI schema 包。
        for (Node child = section.getFirstChild(); child != null; child = child.getNextSibling())
        {
            if (child instanceof Element && name.equals(child.getLocalName()))
            {
                String value = ((Element) child).getAttributeNS(
                        "http://schemas.openxmlformats.org/wordprocessingml/2006/main", attribute);
                return value.isEmpty() ? defaultValue : Double.parseDouble(value);
            }
        }
        return defaultValue;
    }

    private File resolveImageFile(String path)
    {
        String localPath = StringUtils.substringAfter(path, Constants.RESOURCE_PREFIX);
        if (StringUtils.isEmpty(localPath))
        {
            throw new ServiceException("图片路径不正确: " + path);
        }
        return new File(RuoYiConfig.getProfile() + localPath);
    }

    private int getPictureType(String name)
    {
        String lowerName = StringUtils.defaultString(name).toLowerCase();
        if (lowerName.endsWith(".png"))
        {
            return XWPFDocument.PICTURE_TYPE_PNG;
        }
        if (lowerName.endsWith(".gif"))
        {
            return XWPFDocument.PICTURE_TYPE_GIF;
        }
        if (lowerName.endsWith(".bmp"))
        {
            return XWPFDocument.PICTURE_TYPE_BMP;
        }
        return XWPFDocument.PICTURE_TYPE_JPEG;
    }

}
