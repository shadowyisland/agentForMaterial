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
export function getTopTags() {
  return request({
    url: '/system/document/tags/top',
    method: 'get'
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

// 删除文档管理
export function delDocument(documentId) {
  return request({
    url: '/system/document/' + documentId,
    method: 'delete'
  })
}