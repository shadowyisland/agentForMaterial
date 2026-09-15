import request from '@/utils/request'

export function getTemplatePreviewInfo(documentId) {
  return request({
    url: '/system/document/' + documentId + '/preview/template/info',
    method: 'get'
  })
}

export function previewDocumentTemplate(documentId, page) {
  return request({
    url: '/system/document/' + documentId + '/preview/template',
    method: 'get',
    params: { page },
    responseType: 'blob',
    timeout: 60000
  })
}

export function getUploadPreviewInfo(documentId) {
  return request({
    url: '/system/document/' + documentId + '/preview/upload/info',
    method: 'get'
  })
}

export function previewUploadedPage(documentId, page) {
  return request({
    url: '/system/document/' + documentId + '/preview/upload',
    method: 'get',
    params: { page },
    responseType: 'blob',
    timeout: 60000
  })
}

// 查询当前上传文件经 MinerU 解析后保存到 MinIO 的图片。
export function listMineruImages(documentId) {
  return request({
    url: '/system/document/' + documentId + '/mineru-images',
    method: 'get'
  })
}

// 通过后端代理读取当前上传文件的一张 MinerU 图片。
export function readMineruImage(documentId, imageId) {
  return request({
    url: '/system/document/' + documentId + '/mineru-images/' + imageId + '/content',
    method: 'get',
    responseType: 'blob',
    timeout: 60000
  })
}

// 查询文档管理列表
export function listDocument(query) {
  return request({
    url: '/system/document/list',
    method: 'get',
    params: query
  })
}

// 获取常用标签
export function getTopTags(query) {
  return request({
    url: '/system/document/tags/top',
    method: 'get',
    params: query
  })
}

// 获取当前分类统计
export function getDocumentStats(query) {
  return request({
    url: '/system/document/stats',
    method: 'get',
    params: query
  })
}

// 跨分类检索 OCR 正文与使用记录
export function searchDocument(query) {
  return request({
    url: '/system/document/search',
    method: 'get',
    params: query
  })
}

// 查询文档管理详细
export function getDocument(documentId) {
  return request({
    url: '/system/document/' + documentId,
    method: 'get'
  })
}

// 新增文档管理
export function addDocument(data) {
  return request({
    url: '/system/document',
    method: 'post',
    data: data,
    timeout: 360000
  })
}

// 修改文档管理
export function updateDocument(data) {
  return request({
    url: '/system/document',
    method: 'put',
    data: data
  })
}

// 【新增】OCR识别请求
export function ocrDocument(documentId) {
  return request({
    url: '/system/document/ocr/' + documentId,
    method: 'post',
    timeout: 300000,
  })
}

// 获取文档最新 AI 解析结果
export function getLatestExtract(documentId) {
  return request({
    url: '/system/document/' + documentId + '/extract/latest',
    method: 'get',
    timeout: 300000
  })
}

// 手动重新执行 AI 抽取
export function extractDocument(documentId) {
  return request({
    url: '/system/document/' + documentId + '/extract',
    method: 'post',
    timeout: 300000
  })
}

// 保存用户修订后的 JSON
export function saveExtractFinal(documentId, extractId, finalJson) {
  return request({
    url: '/system/document/' + documentId + '/extract/' + extractId + '/final',
    method: 'put',
    data: { finalJson: finalJson }
  })
}

// 保存最终 JSON 并下载回填后的 Word
export function downloadExtract(documentId, extractId, finalJson) {
  return request({
    url: '/system/document/' + documentId + '/extract/' + extractId + '/download',
    method: 'post',
    data: { finalJson: finalJson },
    responseType: 'blob',
    timeout: 300000
  })
}

// 删除文档管理
export function delDocument(documentId) {
  return request({
    url: '/system/document/' + documentId,
    method: 'delete'
  })
}

// 查询文档使用记录
export function listDocumentRecords(documentId) {
  return request({
    url: '/system/document/' + documentId + '/records',
    method: 'get'
  })
}

// 新增文档使用记录
export function addDocumentRecord(documentId, data) {
  return request({
    url: '/system/document/' + documentId + '/records',
    method: 'post',
    data
  })
}

// 修改本人创建的文档使用记录
export function updateDocumentRecord(recordId, data) {
  return request({
    url: '/system/document/records/' + recordId,
    method: 'put',
    data
  })
}

// 管理员删除文档使用记录
export function delDocumentRecord(recordId) {
  return request({
    url: '/system/document/records/' + recordId,
    method: 'delete'
  })
}
