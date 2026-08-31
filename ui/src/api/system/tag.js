import request from '@/utils/request'

// 查询标签列表
export function listTag(query) {
  return request({
    url: '/system/tag/list',
    method: 'get',
    params: query
  })
}

// 新增分类标签（管理员）
export function addTag(data) {
  return request({
    url: '/system/tag',
    method: 'post',
    data
  })
}

// 修改分类标签（管理员）
export function updateTag(data) {
  return request({
    url: '/system/tag',
    method: 'put',
    data
  })
}

// 删除标签
export function delTag(tagId) {
  return request({
    url: '/system/tag/' + tagId,
    method: 'delete'
  })
}

// 修改标签页面开关状态
export function changeTagStatus(tagId, status) {
  return request({
    url: '/system/tag/' + tagId + '/status',
    method: 'put',
    params: { status }
  })
}
