// ============ src/api/doc.js ============
import request from './request'

/**
 * 上传文档
 * @param {File} file 文件对象
 * @param {string} title 标题（可选）
 */
export function uploadDoc(file, title) {
  const formData = new FormData()
  formData.append('file', file)
  if (title) {
    formData.append('title', title)
  }

  return request.post('/doc/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    // 10MB文件上传可能较慢，延长超时
    timeout: 60000
  })
}

/**
 * 获取我的文档列表
 */
export function getDocList() {
  return request.get('/doc/list')
}

/**
 * 获取文档处理状态
 */
export function getDocStatus(docId) {
  return request.get(`/doc/${docId}/status`)
}

/**
 * 删除文档
 */
export function deleteDoc(docId) {
  return request.delete(`/doc/${docId}`)
}

/**
 * 管理员设置文档为全局/取消全局
 * @param {number} docId 文档ID
 * @param {boolean} isGlobal 是否设为全局
 */
export function setDocGlobal(docId, isGlobal) {
  return request.put(`/doc/${docId}/global`, { isGlobal })
}
