-- MinerU ZIP 解析图片元数据。
-- 图片二进制保存在 MinIO，数据库只保存与当前上传文档绑定的对象信息。
ALTER TABLE `sys_document`
    ADD COLUMN `mineru_images` longtext NULL COMMENT 'MinerU 解析图片元数据(JSON)' AFTER `ocr_error`;
