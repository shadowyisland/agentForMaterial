import request from '@/utils/request'

// 查询标签列表
export function listTag(query) {
  return request({
    url: '/system/tag/list',
    method: 'get',
    params: query
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
