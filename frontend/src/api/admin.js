import request from '../api/request'

export function getAllRepos() {
    return request.get('/admin/repos')
}

export function addRepo(data) {
    return request.post('/admin/repo/add', data)
}

export function syncRepo(repoId) {
    return request.post(`/admin/repo/sync/${repoId}`)
}

export function syncAllRepos() {
    return request.post('/admin/repo/sync-all')
}

export function getRepoDocs(repoId) {
    return request.get(`/admin/repo/${repoId}/docs`)
}

export function deleteRepo(repoId) {
    return request.delete(`/admin/repo/${repoId}`)
}
