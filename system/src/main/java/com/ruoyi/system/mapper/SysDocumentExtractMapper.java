package com.ruoyi.system.mapper;

import com.ruoyi.system.domain.SysDocumentExtract;
import org.apache.ibatis.annotations.Param;

/**
 * TDS/MSDS AI 抽取结果 Mapper。
 */
public interface SysDocumentExtractMapper
{
    int insertExtract(SysDocumentExtract extract);

    SysDocumentExtract selectLatestByDocumentId(@Param("documentId") Long documentId,
                                                @Param("documentKind") String documentKind);

    SysDocumentExtract selectById(@Param("documentId") Long documentId,
                                  @Param("extractId") Long extractId,
                                  @Param("documentKind") String documentKind);

    int updateFinalJson(SysDocumentExtract extract);

    int deleteTdsByDocumentIds(@Param("documentIds") Long[] documentIds);

    int deleteMsdsByDocumentIds(@Param("documentIds") Long[] documentIds);
}
