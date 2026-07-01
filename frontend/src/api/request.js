// ============ src/api/request.js ============
import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const request = axios.create({
    baseURL: '/api',
    timeout: 15000
})

// 请求拦截器：每个请求都带 Token
request.interceptors.request.use(
    config => {
        const token = localStorage.getItem('token')
        if (token) {
            config.headers['Authorization'] = 'Bearer ' + token
        }
        // ====== 调试日志 ======
        console.log('========== 请求调试 ==========')
        console.log('[URL]', config.method?.toUpperCase(), config.baseURL + config.url)
        console.log('[Token]', token ? token.substring(0, 30) + '...' : '❌ 无Token！')
        console.log('[Authorization]', config.headers['Authorization'] ? config.headers['Authorization'].substring(0, 40) + '...' : '❌ 无Authorization头')
        console.log('[localStorage全部key]', Object.keys(localStorage))
        console.log('=============================')
        return config
    },
    error => Promise.reject(error)
)

// 响应拦截器
request.interceptors.response.use(
    response => {
        const res = response.data
        if (res.code && res.code !== 200) {
            ElMessage.error(res.message || '请求失败')
            return Promise.reject(new Error(res.message))
        }
        return res
    },
    error => {
        if (error.response) {
            const { status } = error.response
            if (status === 401) {
                ElMessage.error('登录已过期，请重新登录')
                localStorage.removeItem('token')
                router.push('/login')
            } else if (status === 403) {
                ElMessage.error('没有访问权限')
            } else {
                ElMessage.error(error.response.data?.message || '服务器错误')
            }
        } else {
            ElMessage.error('网络错误，请检查连接')
        }
        return Promise.reject(error)
    }
)

export default request
