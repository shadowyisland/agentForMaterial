package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.SysDocumentRecord;

/**
 * 文档使用记录服务。
 */
public interface ISysDocumentRecordService
{
    List<SysDocumentRecord> selectRecordList(Long documentId);

    int insertRecord(SysDocumentRecord record);

    int updateRecord(SysDocumentRecord record);

    int deleteRecord(Long recordId);
}
