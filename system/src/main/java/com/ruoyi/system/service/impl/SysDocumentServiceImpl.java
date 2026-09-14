package com.ruoyi.system.service.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.system.domain.SysDocument;
import com.ruoyi.system.domain.SysDocumentRecord;
import com.ruoyi.system.domain.SysTag;
import com.ruoyi.system.constant.DocumentConstants;
import com.ruoyi.system.mapper.SysDocumentMapper;
import com.ruoyi.system.mapper.SysDocumentExtractMapper;
import com.ruoyi.system.mapper.SysDocumentRecordMapper;
import com.ruoyi.system.mapper.SysTagMapper;
import com.ruoyi.system.service.ISysDocumentService;
import com.ruoyi.system.service.DocumentExtractService;
import com.ruoyi.system.service.DocumentMineruImageService;
import com.ruoyi.system.domain.SysDocumentExtract;

/**
 * 文档管理Service业务层处理
 */
@Service
public class SysDocumentServiceImpl implements ISysDocumentService {

    private static final Logger log = LoggerFactory.getLogger(SysDocumentServiceImpl.class);

    @Autowired
    private SysDocumentMapper documentMapper;

    @Autowired
    private MinerUParseService minerUParseService;

    @Autowired
    private SysTagMapper sysTagMapper; // 引入标签Mapper

    @Autowired
    private SysDocumentRecordMapper recordMapper;

    @Autowired
    private SysDocumentExtractMapper extractMapper;

    @Autowired
    private DocumentExtractService documentExtractService;

    @Autowired
    private DocumentMineruImageService documentMineruImageService;

    @Override
    public SysDocument selectDocumentById(Long documentId) {
        return documentMapper.selectDocumentById(documentId);
    }

    @Override
    public List<SysDocument> selectDocumentList(SysDocument document) {
        normalizeQueryScope(document);
        return documentMapper.selectDocumentList(document);
    }

    @Override
    public List<SysDocument> searchDocumentList(SysDocument document) {
        if (document == null || StringUtils.isEmpty(document.getKeyword())) {
            throw new ServiceException("请输入检索关键词");
        }
        String keyword = document.getKeyword().trim();
        if (keyword.length() > 100) {
            throw new ServiceException("检索关键词不能超过100个字符");
        }
        List<String> rawKeywords = Arrays.stream(keyword.split("\\s+"))
                .filter(StringUtils::isNotEmpty)
                .distinct()
                .limit(10)
                .collect(Collectors.toList());
        if (rawKeywords.isEmpty()) {
            throw new ServiceException("请输入有效的检索关键词");
        }
        String scope = document.getSearchScope();
        if (!DocumentConstants.SEARCH_OCR.equals(scope)
                && !DocumentConstants.SEARCH_RECORD.equals(scope)
                && !DocumentConstants.SEARCH_EXTRACT.equals(scope)) {
            scope = DocumentConstants.SEARCH_ALL;
        }
        document.setSearchScope(scope);
        document.setKeywords(rawKeywords.stream().map(this::escapeLike).collect(Collectors.toList()));

        List<SysDocument> documents = documentMapper.searchDocumentList(document);
        for (SysDocument item : documents) {
            fillMatchMetadata(item, rawKeywords, scope);
        }
        return documents;
    }

    @Override
    public Map<String, Object> selectDocumentStats(SysDocument document) {
        normalizeQueryScope(document);
        return documentMapper.selectDocumentStats(document);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int addDocumentTag(Long documentId, String tagName, Long userId) {
        SysDocument document = documentMapper.selectDocumentById(documentId);
        if (document == null) {
            throw new ServiceException("文档不存在");
        }
        // 1. 获取或创建标签
        Long tagId = getOrCreateTag(tagName, document.getDocumentType(), document.getMaterialCategory(), userId);

        // 2. 检查是否已经关联过该标签，避免重复插入报错
        int count = sysTagMapper.checkDocumentTag(documentId, tagId);
        int rows = 1;
        if (count == 0) {
            // 3. 建立文档和标签的关联
            rows = sysTagMapper.insertDocumentTag(documentId, tagId, DateUtils.getNowDate());
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int replaceDocumentTag(Long documentId, String tagName, Long userId) {
        SysDocument document = documentMapper.selectDocumentById(documentId);
        if (document == null) {
            throw new ServiceException("文档不存在");
        }
        // 1. 删除该文档原有的所有标签关联
        sysTagMapper.deleteDocumentTagByDocumentId(documentId);

        // 2. 获取或创建新标签
        Long tagId = getOrCreateTag(tagName, document.getDocumentType(), document.getMaterialCategory(), userId);

        // 3. 建立新关联
        int rows = sysTagMapper.insertDocumentTag(documentId, tagId, DateUtils.getNowDate());
        return rows;
    }

    /**
     * 内部方法：获取已有标签，不存在则创建
     */
    private Long getOrCreateTag(String tagName, String documentType, String materialCategory, Long userId) {
        String cleanTagName = tagName == null ? "" : tagName.trim();
        SysTag tag = sysTagMapper.selectTagByNameAndScope(cleanTagName, documentType, materialCategory);
        if (tag == null) {
            tag = new SysTag();
            tag.setTagName(cleanTagName);
            tag.setOwnerUserId(userId);
            tag.setDocumentType(documentType);
            tag.setMaterialCategory(materialCategory);
            tag.setStatus("0");
            tag.setCreateBy(SecurityUtils.getUsername());
            tag.setCreateTime(DateUtils.getNowDate());
            // 插入标签，MyBatis的 useGeneratedKeys 会将生成的 tag_id 回填到对象中
            sysTagMapper.insertTag(tag);
        }
        return tag.getTagId();
    }

    /**
     * 新增文档（包含标签处理逻辑）
     */
    @Override
    @Transactional // 开启事务，确保文档和标签同时成功
    public int insertDocument(SysDocument document) {
        normalizeDocumentScope(document);
        document.setCreateUserId(SecurityUtils.getUserId());
        document.setCreateTime(DateUtils.getNowDate());
        // 1. 保存文档基础信息
        int rows = documentMapper.insertDocument(document);

        // 2. 处理标签逻辑
        if (document.getTags() != null && !document.getTags().isEmpty()) {
            insertTags(document.getTags(), document, document.getCreateBy());
        }

        // 文档和标签先落库。内部文档执行 OCR + AI，外部文档只执行 OCR。
        autoParse(document);
        return rows;
    }

    /**
     * 修改文档（包含标签更新逻辑）
     */
    @Override
    @Transactional
    public int updateDocument(SysDocument document) {
        SysDocument current = documentMapper.selectDocumentById(document.getDocumentId());
        if (current == null) {
            throw new ServiceException("文档不存在");
        }
        // 文档所属范围由上传入口确定，普通编辑不能跨分类移动。
        document.setDocumentType(current.getDocumentType());
        document.setMaterialCategory(current.getMaterialCategory());
        if (DocumentConstants.TYPE_INTERNAL.equals(current.getDocumentType())) {
            normalizeDocumentKind(document);
            document.setSourceName("");
            document.setSourceUrl("");
            document.setPublishDate(null);
        } else {
            document.setDocumentKind(null);
        }
        document.setUpdateTime(DateUtils.getNowDate());
        int rows = documentMapper.updateDocument(document);

        // 3. 处理标签更新逻辑
        // 只有当前端传来了 tags 字段（哪怕是空数组），才进行标签更新
        if (document.getTags() != null) {
            // A. 先删除该文档关联的所有旧标签
            sysTagMapper.deleteDocTagByDocId(document.getDocumentId());

            // B. 如果有新标签，则重新插入
            if (!document.getTags().isEmpty()) {
                insertTags(document.getTags(), current, document.getUpdateBy());
            }
        }

        return rows;
    }

    /**
     * 公用方法：插入标签并关联
     */
    private void insertTags(List<String> tags, SysDocument document, String createBy) {
        Long userId = SecurityUtils.getUserId(); // 获取当前登录用户

        for (String tagName : tags) {
            if (StringUtils.isEmpty(tagName)) continue;

            // 生成标准化key (去除空格，转小写) 用于查重
            String cleanTagName = tagName.trim();
            String tagKey = cleanTagName.toLowerCase().replaceAll("\\s+", "");

            // A. 检查标签是否已存在
            SysTag tag = sysTagMapper.checkTagUnique(document.getDocumentType(), document.getMaterialCategory(), tagKey);
            Long tagId;

            if (tag == null) {
                // B. 不存在，创建新标签
                tag = new SysTag();
                tag.setOwnerUserId(userId);
                tag.setTagName(cleanTagName);
                tag.setTagKey(tagKey);
                tag.setDocumentType(document.getDocumentType());
                tag.setMaterialCategory(document.getMaterialCategory());
                tag.setStatus("0");
                tag.setCreateBy(createBy);
                sysTagMapper.insertTag(tag);
                tagId = tag.getTagId();
            } else {
                tagId = tag.getTagId();
            }

            // C. 在中间表中建立关联
            sysTagMapper.insertDocTag(document.getDocumentId(), tagId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteDocumentByIds(Long[] documentIds) {
        List<SysDocument> documents = new ArrayList<SysDocument>();
        for (Long documentId : documentIds) {
            SysDocument document = documentMapper.selectDocumentById(documentId);
            if (document != null) {
                documents.add(document);
            }
        }
        sysTagMapper.deleteDocTagByDocIds(documentIds);
        documentExtractService.deleteByDocumentIds(documentIds);
        for (SysDocument document : documents) {
            documentMineruImageService.deleteImages(document);
        }
        int rows = documentMapper.deleteDocumentByIds(documentIds);
        deleteLocalFiles(documents);
        return rows;
    }

    private void deleteLocalFiles(List<SysDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }
        for (SysDocument document : documents) {
            String filePath = document.getFilePath();
            if (StringUtils.isEmpty(filePath) || !filePath.contains(Constants.RESOURCE_PREFIX)) {
                continue;
            }
            String localPath = RuoYiConfig.getProfile() + StringUtils.substringAfter(filePath, Constants.RESOURCE_PREFIX);
            FileUtils.deleteFile(localPath);
        }
    }

    /**
     * OCR 识别逻辑
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

        try {
            MinerUParseResult result = minerUParseService.parse(localPath, doc.getFileOriginName());
            documentMineruImageService.replaceImages(doc, result.getImages());
            // Markdown 与当前上传文档解析出的图片一起完成保存。
            doc.setIsRecognized(1);
            doc.setOcrContent(result.getMarkdownContent());
            doc.setOcrTime(DateUtils.getNowDate());
            doc.setOcrError("");

            return documentMapper.updateDocument(doc);
        } catch (Exception e) {
            doc.setIsRecognized(2);
            doc.setOcrError(StringUtils.substring(e.getMessage(), 0, 500));
            doc.setOcrTime(DateUtils.getNowDate());
            documentMapper.updateDocument(doc);
            throw new ServiceException("识别过程中发生错误: " + e.getMessage());
        }
    }

    private void normalizeQueryScope(SysDocument document) {
        if (document == null) {
            return;
        }
        if (StringUtils.isEmpty(document.getDocumentType())) {
            return;
        }
        String documentType = document.getDocumentType().toUpperCase(Locale.ROOT);
        if (!DocumentConstants.TYPE_INTERNAL.equals(documentType) && !DocumentConstants.TYPE_EXTERNAL.equals(documentType)) {
            throw new ServiceException("文档类型不正确");
        }
        document.setDocumentType(documentType);
        if (DocumentConstants.TYPE_INTERNAL.equals(documentType)) {
            if (StringUtils.isEmpty(document.getMaterialCategory()) || !DocumentConstants.MATERIAL_CATEGORIES.contains(document.getMaterialCategory())) {
                throw new ServiceException("材料分类不正确");
            }
        } else {
            document.setMaterialCategory(null);
        }
    }

    private void normalizeDocumentScope(SysDocument document) {
        if (StringUtils.isEmpty(document.getDocumentType())) {
            document.setDocumentType(DocumentConstants.TYPE_INTERNAL);
        }
        document.setDocumentType(document.getDocumentType().toUpperCase(Locale.ROOT));
        if (DocumentConstants.TYPE_INTERNAL.equals(document.getDocumentType())) {
            if (StringUtils.isEmpty(document.getMaterialCategory())) {
                document.setMaterialCategory(DocumentConstants.CATEGORY_RAW_MATERIAL);
            }
            if (!DocumentConstants.MATERIAL_CATEGORIES.contains(document.getMaterialCategory())) {
                throw new ServiceException("材料分类不正确");
            }
            document.setSourceName(null);
            document.setSourceUrl(null);
            document.setPublishDate(null);
        } else if (DocumentConstants.TYPE_EXTERNAL.equals(document.getDocumentType())) {
            document.setMaterialCategory(null);
            document.setDocumentKind(null);
        } else {
            throw new ServiceException("文档类型不正确");
        }
        normalizeDocumentKind(document);
    }

    private void normalizeDocumentKind(SysDocument document) {
        if (StringUtils.isEmpty(document.getDocumentKind())) {
            return;
        }
        String documentKind = document.getDocumentKind().toUpperCase(Locale.ROOT);
        if (!DocumentConstants.DOCUMENT_KINDS.contains(documentKind)) {
            throw new ServiceException("资料类型仅支持 TDS 或 MSDS");
        }
        document.setDocumentKind(documentKind);
    }

    private void autoParse(SysDocument document) {
        try {
            ocrDocument(document.getDocumentId());
            if (DocumentConstants.TYPE_INTERNAL.equals(document.getDocumentType())) {
                documentExtractService.extractDocument(document.getDocumentId());
            }
        } catch (Exception e) {
            // OCR 或提示词未配置时，文档仍应保留在管理列表中。
            log.warn("文档自动解析未完成，documentId: {}, 原因: {}", document.getDocumentId(), e.getMessage());
        }
    }

    private String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private void fillMatchMetadata(SysDocument document, List<String> keywords, String scope) {
        String ocr = StringUtils.defaultString(document.getOcrContent());
        List<SysDocumentRecord> records = recordMapper.selectRecordListByDocumentId(document.getDocumentId());
        StringBuilder recordText = new StringBuilder();
        for (SysDocumentRecord record : records) {
            recordText.append(StringUtils.defaultString(record.getUsageContent())).append(' ')
                    .append(StringUtils.defaultString(record.getRemark())).append(' ');
        }

        boolean ocrMatched = containsAny(ocr, keywords);
        boolean recordMatched = containsAny(recordText.toString(), keywords);
        String extractText = latestExtractContent(document);
        boolean extractMatched = containsAny(extractText, keywords);
        if (DocumentConstants.SEARCH_OCR.equals(scope)) {
            document.setMatchSource("OCR正文");
            document.setMatchSnippet(buildSnippet(ocr, keywords));
        } else if (DocumentConstants.SEARCH_RECORD.equals(scope)) {
            document.setMatchSource("使用记录与备注");
            document.setMatchSnippet(buildSnippet(recordText.toString(), keywords));
        } else if (DocumentConstants.SEARCH_EXTRACT.equals(scope)) {
            document.setMatchSource("AI 提取内容");
            document.setMatchSnippet(buildSnippet(extractText, keywords));
        } else {
            List<String> sources = new ArrayList<String>();
            if (ocrMatched) sources.add("OCR正文");
            if (recordMatched) sources.add("使用记录与备注");
            if (extractMatched) sources.add("AI 提取内容");
            document.setMatchSource(String.join(" / ", sources));
            String matchedContent = ocrMatched ? ocr : (recordMatched ? recordText.toString() : extractText);
            document.setMatchSnippet(buildSnippet(matchedContent, keywords));
        }
        // 搜索结果只返回摘要，避免列表接口携带整篇OCR正文。
        document.setOcrContent(null);
    }

    private String latestExtractContent(SysDocument document) {
        if (!DocumentConstants.TYPE_INTERNAL.equals(document.getDocumentType())
                || !DocumentConstants.DOCUMENT_KINDS.contains(document.getDocumentKind())) {
            return "";
        }
        SysDocumentExtract extract = extractMapper.selectLatestByDocumentId(
                document.getDocumentId(), document.getDocumentKind());
        if (extract == null) {
            return "";
        }
        return StringUtils.isNotEmpty(extract.getFinalJson()) ? extract.getFinalJson() : extract.getAiJson();
    }

    private boolean containsAny(String content, List<String> keywords) {
        String normalized = StringUtils.defaultString(content).toLowerCase(Locale.ROOT);
        for (String keyword : keywords) {
            if (normalized.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String buildSnippet(String content, List<String> keywords) {
        String compact = StringUtils.defaultString(content).replaceAll("\\s+", " ").trim();
        if (compact.length() <= 220) {
            return compact;
        }
        String lower = compact.toLowerCase(Locale.ROOT);
        StringBuilder snippet = new StringBuilder();
        int lastEnd = -1;
        for (String keyword : keywords) {
            int index = lower.indexOf(keyword.toLowerCase(Locale.ROOT));
            if (index < 0) {
                continue;
            }
            int start = Math.max(0, index - 45);
            int end = Math.min(compact.length(), index + keyword.length() + 75);
            if (start <= lastEnd) {
                continue;
            }
            if (snippet.length() > 0) {
                snippet.append(" … ");
            }
            snippet.append(start > 0 ? "…" : "").append(compact, start, end)
                    .append(end < compact.length() ? "…" : "");
            lastEnd = end;
            if (snippet.length() >= 420) {
                break;
            }
        }
        return snippet.length() == 0 ? compact.substring(0, 220) + "…" : snippet.toString();
    }
}
