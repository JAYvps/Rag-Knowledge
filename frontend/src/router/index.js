// ============ src/router/index.js（加入对话路由） ============
import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'
import component from 'element-plus/es/components/tree-select/src/tree-select-option.mjs'

const routes = [
    {
        path: '/admin/sync',
        title: '同步管理',
        icon: 'Setting',
        name: 'SyncManage',
        component: () => import('../views/admin/SyncManageView.vue'),
        meta: { title: '同步管理' }
    },
    {
        path: '/admin/sync',
        name: 'SyncManage',
        component: () => import('../views/admin/SyncManageView.vue'),
        meta: { title: '同步管理' }
    }

    ,
    {
        path: '/login',
        name: 'Login',
        component: () => import('../views/auth/LoginView.vue'),
        meta: { requiresAuth: false }
    },
    {
        path: '/register',
        name: 'Register',
        component: () => import('../views/auth/RegisterView.vue'),
        meta: { requiresAuth: false }
    },
    {
        path: '/',
        component: () => import('../components/AppLayout.vue'),
        meta: { requiresAuth: true },
        children: [
            {
                path: 'docs',
                name: 'MyDocs',
                component: () => import('../views/doc/MyDocsView.vue')
            },
            {
                path: '',
                name: 'Home',
                component: () => import('../views/HomeView.vue')
            },
            {
                path: 'kb/:repoId?',
                name: 'Kb',
                component: () => import('../views/kb/KbView.vue')
            },
            {
                path: 'chat/:conversationId?',
                name: 'Chat',
                component: () => import('../views/chat/ChatView.vue')
            }
        ]
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

router.beforeEach((to, from, next) => {
    const userStore = useUserStore()
    if (to.meta.requiresAuth !== false && !userStore.token) {
        next('/login')
    } else if (to.path === '/login' && userStore.token) {
        next('/')
    } else {
        next()
    }
})

export default router
