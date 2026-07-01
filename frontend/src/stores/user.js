// ============ src/stores/user.js ============
import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
    const token = ref(localStorage.getItem('token') || '')
    const userId = ref(localStorage.getItem('userId') || '')
    const username = ref(localStorage.getItem('username') || '')
    const role = ref(localStorage.getItem('role') || '')

    function setLoginInfo(data) {
        token.value = data.token
        userId.value = data.userId
        username.value = data.username
        role.value = data.role

        localStorage.setItem('token', data.token)
        localStorage.setItem('userId', data.userId)
        localStorage.setItem('username', data.username)
        localStorage.setItem('role', data.role)
    }

    function logout() {
        token.value = ''
        userId.value = ''
        username.value = ''
        role.value = ''
        localStorage.clear()
    }

    return { token, userId, username, role, setLoginInfo, logout }
})
