package com.ruoyi.system.service.impl;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.SysTag;
import com.ruoyi.system.mapper.SysTagMapper;
import com.ruoyi.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.SysDocumentMapper;
import com.ruoyi.system.domain.SysDocument;
import com.ruoyi.system.service.ISysDocumentService;
import com.ruoyi.common.exception.ServiceException;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.ocr.BaiduOcrService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * 文档管理Service业务层处理
 */
@Service
public class SysDocumentServiceImpl implements ISysDocumentService {

    @Autowired
    private SysDocumentMapper documentMapper;

    // 注入百度OCR服务
    @Autowired
    private BaiduOcrService baiduOcrService;

    @Autowired
    private SysTagMapper sysTagMapper; // 引入标签Mapper

    @Override
    public SysDocument selectDocumentById(Long documentId) {
        return documentMapper.selectDocumentById(documentId);
    }

    @Override
    public List<SysDocument> selectDocumentList(SysDocument document) {
        return documentMapper.selectDocumentList(document);
    }

    /**
     * 新增文档（包含标签处理逻辑）
     */
    @Override
    @Transactional // 开启事务，确保文档和标签同时成功
    public int insertDocument(SysDocument document) {
        document.setCreateTime(DateUtils.getNowDate());
        // 1. 保存文档基础信息
        int rows = documentMapper.insertDocument(document);

        // 2. 处理标签逻辑 (新增部分)
        if (document.getTags() != null && !document.getTags().isEmpty()) {
            Long userId = SecurityUtils.getUserId(); // 获取当前登录用户

            for (String tagName : document.getTags()) {
                if (StringUtils.isEmpty(tagName)) continue;

                // 生成标准化key (去除空格，转小写) 用于查重
                String cleanTagName = tagName.trim();
                String tagKey = cleanTagName.toLowerCase().replaceAll("\\s+", "");

                // A. 检查标签是否已存在
                SysTag tag = sysTagMapper.checkTagUnique(userId, tagKey);
                Long tagId;

                if (tag == null) {
                    // B. 不存在，创建新标签
                    tag = new SysTag();
                    tag.setOwnerUserId(userId);
                    tag.setTagName(cleanTagName);
                    tag.setTagKey(tagKey); // 记得存入 key
                    tag.setCreateBy(document.getCreateBy());
                    sysTagMapper.insertTag(tag);
                    tagId = tag.getTagId(); // 获取回填的主键
                } else {
                    // C. 存在，直接使用ID
                    tagId = tag.getTagId();
                }

                // D. 在中间表中建立关联
                sysTagMapper.insertDocTag(document.getDocumentId(), tagId);
            }
        }
        return rows;
    }

    @Override
    public int updateDocument(SysDocument document) {
        document.setUpdateTime(DateUtils.getNowDate());
        // 注意：如果修改文档时也允许修改标签，这里也需要加类似的标签处理逻辑
        // 目前版本暂只处理新增时的标签
        return documentMapper.updateDocument(document);
    }

    @Override
    public int deleteDocumentByIds(Long[] documentIds) {
        // 建议：删除文档时，最好同时也删除 sys_document_tag 表中的关联记录
        // sysTagMapper.deleteDocTagByDocIds(documentIds);
        return documentMapper.deleteDocumentByIds(documentIds);
    }

    /**
     * OCR 识别逻辑 (保留你原有的代码)
     */
    @Override
    public int ocrDocument(Long documentId) {
        SysDocument doc = documentMapper.selectDocumentById(documentId);
        if (doc == null) {
            throw new ServiceException("文档不存在");
        }

        String filePath = doc.getFilePath();
        if (StringUtils.isEmpty(filePath)) {
            throw new ServiceException("文件路径为空，无法识别");
        }

        // 1. 获取本地绝对路径
        String localPath = RuoYiConfig.getProfile() + StringUtils.substringAfter(filePath, Constants.RESOURCE_PREFIX);
        File file = new File(localPath);
        if (!file.exists()) {
            throw new ServiceException("文件不存在: " + localPath);
        }

        String resultContent = "";

        try {
            // 2. 判断是否为 PDF
            if (localPath.toLowerCase().endsWith(".pdf")) {
                // 如果是 PDF，调用 PDF 处理逻辑
                resultContent = processPdfAndOcr(file);
            } else {
                // 如果是普通图片，直接调用 OCR
                resultContent = baiduOcrService.recognizeGeneral(localPath);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException("识别过程中发生错误: " + e.getMessage());
        }

            // 模拟返回结果
            String mockResult = "【开发模式】这是模拟的OCR识别结果。\n" +
                    "文档ID: " + documentId + "\n" +
                    "识别时间: " + DateUtils.getTime();

            // 更新数据库状态
            doc.setIsRecognized(1); // 1表示已识别
            doc.setOcrContent(mockResult);
            // 注意：SysDocument 实体类里要有 ocrTime 和 ocrError 字段，否则这里会爆红
            // 如果实体类还没加这俩字段，请先注释掉下面两行
            // doc.setOcrTime(DateUtils.getNowDate());
            // doc.setOcrError(null);
        // 3. 更新数据库
        doc.setIsRecognized(1);
        doc.setOcrContent(resultContent);
        doc.setOcrTime(DateUtils.getNowDate());

        return documentMapper.updateDocument(doc);
    }
    /**
     * 辅助方法：处理 PDF 文件 (拆分 -> 转图 -> 识别 -> 拼接)
     */
    private String processPdfAndOcr(File pdfFile) throws IOException {
        StringBuilder fullText = new StringBuilder();
        PDDocument document = null;

        try {
            // 加载 PDF
            document = PDDocument.load(pdfFile);
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            int pageCount = document.getNumberOfPages();

            // 限制最大页数，防止 PDF 过大导致超时 (可选)
            if (pageCount > 20) {
                fullText.append("[警告] PDF页数过多(").append(pageCount).append(")，仅识别前 20 页...\n\n");
                pageCount = 20;
            }

            // 循环处理每一页
            for (int i = 0; i < pageCount; i++) {
                // 1. 将 PDF 页渲染为图片 (300 DPI 清晰度较高，适合 OCR)
                BufferedImage bim = pdfRenderer.renderImageWithDPI(i, 300, ImageType.RGB);

                // 2. 创建临时文件保存图片
                File tempFile = File.createTempFile("ocr_temp_page_" + i, ".jpg");
                ImageIO.write(bim, "jpg", tempFile);

                // 3. 调用 OCR 识别该临时图片
                String pageResult = baiduOcrService.recognizeGeneral(tempFile.getAbsolutePath());

                // 4. 拼接结果
                fullText.append("=== 第 ").append(i + 1).append(" 页 ===\n");
                fullText.append(pageResult).append("\n\n");

                // 5. 删除临时文件
                tempFile.delete();
            }
        } catch (Exception e) {
            fullText.append("PDF 解析失败: ").append(e.getMessage());
            throw e;
        } finally {
            if (document != null) {
                document.close();
            }
        }

        return fullText.toString();
    }
}