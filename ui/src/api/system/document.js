import request from '@/utils/request'

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
    data: data
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
    timeout: 100000,
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
