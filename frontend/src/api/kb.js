// ============ src/api/kb.js ============
import request from './request'

/**
 * 获取知识库列表
 */
export function getRepoList() {
    return request.get('/kb/repos')
}

/**
 * 获取某个知识库的文档列表
 * @param {number} repoId 知识库ID
 */
export function getDocList(repoId) {
    return request.get(`/kb/${repoId}/docs`)
}
