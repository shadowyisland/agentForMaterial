package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.SysDocumentRecord;

/**
 * 文档使用记录Mapper。
 */
public interface SysDocumentRecordMapper
{
    List<SysDocumentRecord> selectRecordListByDocumentId(Long documentId);

    SysDocumentRecord selectRecordById(Long recordId);

    int insertRecord(SysDocumentRecord record);

    int updateRecord(SysDocumentRecord record);

    int softDeleteRecord(SysDocumentRecord record);
}
