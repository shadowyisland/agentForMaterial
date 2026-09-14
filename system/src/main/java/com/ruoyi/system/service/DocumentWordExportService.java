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
    private static final String PICTOGRAM_IMAGE = "{{@象形图}}";
    private static final String PERSONAL_PROTECTION_IMAGE = "{{@个人防护装备总要求}}";
    private static final String TRANSPORT_LABEL_IMAGE = "{{@运输标签}}";
    /** 2.05cm，图片宽度根据原始纵横比自动计算。 */
    private static final double IMAGE_FIXED_HEIGHT_POINTS = 72.0 * 2.05 / 2.54;

    @Autowired
    private DocumentTemplateResolver documentTemplateResolver;

    @Autowired
    private DocumentMineruImageService documentMineruImageService;

    public void writeDocument(SysDocument document, String finalJson, OutputStream outputStream)
    {
        JSONObject root = parseJson(finalJson);
        Resource template = documentTemplateResolver.resolve(document.getMaterialCategory(), document.getDocumentKind());
        root.put("文档名称", StringUtils.defaultString(document.getDocumentName()));
        root.putIfAbsent("产品名称", StringUtils.defaultString(document.getProductName()));
        root.put("产品型号", StringUtils.defaultString(document.getProductModel()));
        root.put("内部编号", StringUtils.defaultString(document.getInternalCode()));
        normalizeMsds(document, root);
        normalizeFillerSolventRows(document, root);
        try (InputStream input = template.getInputStream())
        {
            DocumentWordTemplateRenderer renderer = new DocumentWordTemplateRenderer();
            JSONArray images = root.getJSONArray("图片");
            if (images != null && !images.isEmpty())
            {
                // 图片先写入专用标记所在段落，再渲染文字，避免按标题插入时改变位置。
                try (XWPFDocument word = new XWPFDocument(input))
                {
                    insertSelectedImages(document, word, images);
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

    /** MSDS 模板直接使用法规章节层级；这里只处理网页数组和固定 Word 表格行之间的映射。 */
    private void normalizeMsds(SysDocument document, JSONObject root)
    {
        if (!"MSDS".equals(document.getDocumentKind())) return;
        JSONObject section1 = root.getJSONObject("第1部分 物质或混合物和供应商的标识");
        if (section1 == null)
        {
            // 兼容历史解析结果：旧记录将第 1 部分字段直接放在根节点。
            section1 = new JSONObject();
            String[] section1Keys = { "产品中文名称", "产品英文名称", "产品编号", "CAS No.", "EC No.", "分子式",
                    "REACH注册号", "UFI", "产品的推荐用途", "产品的限制用途", "企业名称", "企业地址", "邮编",
                    "联系电话", "电子邮箱", "应急电话", "响应时间" };
            for (String key : section1Keys)
            {
                if (root.containsKey(key)) section1.put(key, root.get(key));
            }
            root.put("第1部分 物质或混合物和供应商的标识", section1);
        }
        String[] section1Keys = { "产品中文名称", "产品英文名称", "产品编号", "CAS No.", "EC No.", "分子式",
                "REACH注册号", "UFI", "产品的推荐用途", "产品的限制用途", "企业名称", "企业地址", "邮编",
                "联系电话", "电子邮箱", "应急电话", "响应时间" };
        for (String key : section1Keys)
        {
            if (!section1.containsKey(key) && root.containsKey(key)) section1.put(key, root.get(key));
        }
        for (String key : section1.keySet()) root.putIfAbsent(key, section1.get(key));
        JSONObject section4 = root.getJSONObject("第4部分 急救措施");
        if (section4 != null)
        {
            // 网页编辑器把急救措施维护为“类别/内容”数组，原始 Word 是固定类别的表格行。
            // 在下载前补上同名字段，表格即可保留标准模板的行、边框和顺序。
            copyCategoryContents(section4, "急救措施描述");
        }
        JSONObject section5 = root.getJSONObject("第5部分 消防措施");
        if (section5 != null)
        {
            copyCategoryContents(section5, "灭火介质");
        }
        JSONObject section2 = root.getJSONObject("第2部分 危险标识");
        if (section2 != null)
        {
            JSONArray classifications = section2.getJSONArray("依据欧盟 CLP 法规[（EC）No 1272/2008]的危险性分类");
            if (onlyNoDataClassifications(classifications))
            {
                // “无数据资料”不是危险性分类，空表不应在成品中保留。
                section2.put("依据欧盟 CLP 法规[（EC）No 1272/2008]的危险性分类", new JSONArray());
            }
        }
        JSONObject section16 = root.getJSONObject("第16部分 其他信息");
        if (section16 == null) section16 = new JSONObject();
        section16.putIfAbsent("编制日期", root.get("编制日期"));
        section16.putIfAbsent("修订日期", root.get("修订日期"));
        root.put("第16部分 其他信息", section16);
    }

    private boolean onlyNoDataClassifications(JSONArray values)
    {
        if (values == null || values.isEmpty())
        {
            return false;
        }
        for (int i = 0; i < values.size(); i++)
        {
            JSONObject value = values.getJSONObject(i);
            String category = value == null ? null : value.getString("分类");
            if (!StringUtils.isEmpty(category) && !"无数据资料".equals(category) && !"无资料".equals(category)
                    && !"不适用".equals(category))
            {
                return false;
            }
        }
        return true;
    }

    private void copyCategoryContents(JSONObject section, String arrayName)
    {
        JSONArray values = section.getJSONArray(arrayName);
        if (values == null)
        {
            return;
        }
        for (int i = 0; i < values.size(); i++)
        {
            JSONObject value = values.getJSONObject(i);
            if (value == null)
            {
                continue;
            }
            String category = value.getString("类别");
            if (StringUtils.isEmpty(category))
            {
                continue;
            }
            section.putIfAbsent(category, StringUtils.defaultString(value.getString("内容")));
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

    private void insertSelectedImages(SysDocument document, XWPFDocument word, JSONArray images)
    {
        for (int i = 0; i < images.size(); i++)
        {
            JSONObject image = images.getJSONObject(i);
            if (image == null
                    || (StringUtils.isEmpty(image.getString("路径")) && StringUtils.isEmpty(image.getString("图片ID"))))
            {
                continue;
            }
            String marker = imageMarker(image);
            if (marker == null)
            {
                continue;
            }
            for (XWPFParagraph paragraph : allParagraphs(word))
            {
                if (marker.equals(paragraph.getText().trim()))
                {
                    addPicture(document, word, paragraph, image);
                    break;
                }
            }
        }
    }

    private String imageMarker(JSONObject image)
    {
        String type = image.getString("类型");
        if ("分子结构".equals(type) || "分子结构后".equals(image.getString("位置"))) return STRUCTURE_IMAGE;
        if ("象形图".equals(type)) return PICTOGRAM_IMAGE;
        if ("个人防护装备总要求".equals(type)) return PERSONAL_PROTECTION_IMAGE;
        if ("运输标签".equals(type)) return TRANSPORT_LABEL_IMAGE;
        return null;
    }

    private java.util.List<XWPFParagraph> allParagraphs(XWPFDocument word)
    {
        java.util.List<XWPFParagraph> paragraphs = new java.util.ArrayList<XWPFParagraph>(word.getParagraphs());
        collectTableParagraphs(word.getTables(), paragraphs);
        return paragraphs;
    }

    private void collectTableParagraphs(java.util.List<org.apache.poi.xwpf.usermodel.XWPFTable> tables,
            java.util.List<XWPFParagraph> paragraphs)
    {
        for (org.apache.poi.xwpf.usermodel.XWPFTable table : tables)
            for (org.apache.poi.xwpf.usermodel.XWPFTableRow row : table.getRows())
                for (org.apache.poi.xwpf.usermodel.XWPFTableCell cell : row.getTableCells())
                {
                    paragraphs.addAll(cell.getParagraphs());
                    collectTableParagraphs(cell.getTables(), paragraphs);
                }
    }

    private void addPicture(SysDocument document, XWPFDocument word, XWPFParagraph paragraph, JSONObject imageItem)
    {
        String pictureName = imageItem.getString("名称");
        byte[] imageBytes = readImageBytes(document, imageItem);
        try (InputStream inputStream = new ByteArrayInputStream(imageBytes))
        {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image == null)
            {
                throw new ServiceException("图片格式不正确: " + pictureName);
            }
            // 图片可能来自 MinIO 或本地上传，但最终都必须按组件尺寸插入。
            // 与 Word 的“锁定纵横比”一致：单张图片高度固定 2.05cm，宽度随原图比例变化。
            double scale = IMAGE_FIXED_HEIGHT_POINTS / image.getHeight();
            double width = image.getWidth() * scale;
            double height = IMAGE_FIXED_HEIGHT_POINTS;
            XWPFRun run = paragraph.createRun();
            String name = StringUtils.isEmpty(pictureName) ? "image.png" : pictureName;
            run.addPicture(inputStream, getPictureType(name), name,
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

    private byte[] readImageBytes(SysDocument document, JSONObject imageItem)
    {
        String imageId = imageItem.getString("图片ID");
        if (StringUtils.isNotEmpty(imageId))
        {
            return documentMineruImageService.readImage(document, imageId);
        }
        String path = imageItem.getString("路径");
        File imageFile = resolveImageFile(path);
        if (!imageFile.isFile())
        {
            throw new ServiceException("图片文件不存在: " + path);
        }
        try (InputStream inputStream = new FileInputStream(imageFile))
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
        catch (Exception e)
        {
            throw new ServiceException("读取图片失败: " + e.getMessage());
        }
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
