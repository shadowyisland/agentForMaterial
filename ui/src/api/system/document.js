import request from '@/utils/request'

// 查询文档列表
export function listDocument(query) {
  return request({
    url: '/system/document/list',
    method: 'get',
    params: query
  })
}

// 新增文档
export function addDocument(data) {
  return request({
    url: '/system/document',
    method: 'post',
    data: data
  })
}

// 删除文档
export function delDocument(documentId) {
  return request({
    url: '/system/document/' + documentId,
    method: 'delete'
  })
}

// 【新增】OCR识别请求
export function ocrDocument(documentId) {
  return request({
    url: '/system/document/ocr/' + documentId,
    method: 'post'
  })
}

// 查询文档管理详细
export function getDocument(documentId) {
  return request({
    url: '/system/document/' + documentId,
    method: 'get'
  })
}
