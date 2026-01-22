package com.ruoyi.system.service.impl;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.SysDocumentMapper;
import com.ruoyi.system.domain.SysDocument;
import com.ruoyi.system.service.ISysDocumentService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.ocr.BaiduOcrService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import java.io.IOException;

import javax.imageio.ImageIO;

@Service
public class SysDocumentServiceImpl implements ISysDocumentService
{
    @Autowired
    private SysDocumentMapper documentMapper;

    // 注入百度OCR服务
    @Autowired
    private BaiduOcrService baiduOcrService;

    @Override
    public SysDocument selectDocumentById(Long documentId) {
        return documentMapper.selectDocumentById(documentId);
    }

    @Override
    public List<SysDocument> selectDocumentList(SysDocument document) {
        return documentMapper.selectDocumentList(document);
    }

    @Override
    public int insertDocument(SysDocument document) {
        document.setCreateTime(DateUtils.getNowDate());
        return documentMapper.insertDocument(document);
    }

    @Override
    public int updateDocument(SysDocument document) {
        document.setUpdateTime(DateUtils.getNowDate());
        return documentMapper.updateDocument(document);
    }

    @Override
    public int deleteDocumentByIds(Long[] documentIds) {
        return documentMapper.deleteDocumentByIds(documentIds);
    }

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