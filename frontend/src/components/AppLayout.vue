<!-- ============ src/components/AppLayout.vue ============ -->
<!--
  Clean minimal layout:
  - Neutral sidebar with subtle border
  - Emerald accent for active state
  - No particles, no purple gradients, no glowing effects
  - Spring-physics indicator animation
-->
<template>
  <el-container class="app-layout">
    <!-- Sidebar -->
    <el-aside width="240px" class="sidebar" ref="sidebarRef">
      <!-- Logo -->
      <div class="logo" ref="logoRef">
        <div class="logo-mark">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <rect width="24" height="24" rx="6" fill="var(--accent)"/>
            <path d="M7 9h10M7 12h7M7 15h9" stroke="#fff" stroke-width="1.8" stroke-linecap="round"/>
          </svg>
        </div>
        <span class="logo-text">RAG 知识库</span>
      </div>

      <!-- Navigation -->
      <div class="nav-wrapper">
        <div class="nav-indicator" ref="indicatorRef"></div>
        <el-menu
            :default-active="activeMenu"
            router
            background-color="transparent"
            text-color="var(--text-secondary)"
            active-text-color="var(--text-primary)"
            class="sidebar-menu"
            @select="moveIndicator"
        >
          <el-menu-item index="/">
            <el-icon><HomeFilled /></el-icon>
            <span>首页</span>
          </el-menu-item>

          <el-menu-item index="/kb">
            <el-icon><Collection /></el-icon>
            <span>知识库</span>
          </el-menu-item>

          <el-menu-item index="/docs">
            <el-icon><Document /></el-icon>
            <span>我的文档</span>
          </el-menu-item>

          <el-menu-item index="/chat">
            <el-icon><ChatDotRound /></el-icon>
            <span>智能问答</span>
          </el-menu-item>

          <el-menu-item v-if="userStore.role === 'ADMIN'" index="/admin/sync">
            <el-icon><Setting /></el-icon>
            <span>同步管理</span>
          </el-menu-item>
        </el-menu>
      </div>

      <!-- Footer -->
      <div class="sidebar-footer">
        <div class="footer-user">
          <div class="user-avatar">{{ userStore.username?.charAt(0)?.toUpperCase() }}</div>
          <span class="user-name">{{ userStore.username }}</span>
        </div>
      </div>
    </el-aside>

    <!-- Main area -->
    <el-container>
      <!-- Header -->
      <el-header class="header" height="56px">
        <div class="header-left">
          <h1 class="page-title">{{ currentTitle }}</h1>
        </div>
        <div class="header-right">
          <span class="current-user">{{ userStore.username }}</span>
          <el-button text class="logout-btn" @click="handleLogout">
            <el-icon><SwitchButton /></el-icon>
            退出
          </el-button>
        </div>
      </el-header>

      <!-- Content -->
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import gsap from 'gsap'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const sidebarRef = ref(null)
const logoRef = ref(null)
const indicatorRef = ref(null)
let ctx

const activeMenu = computed(() => route.path)

const currentTitle = computed(() => {
  const titles = { '/': '首页', '/kb': '知识库', '/docs': '我的文档', '/chat': '智能问答', '/admin/sync': '同步管理' }
  return titles[route.path] || 'RAG 知识库'
})

function handleLogout() {
  userStore.logout()
  router.push('/login')
}

function moveIndicator() {
  nextTick(() => {
    const activeItem = document.querySelector('.sidebar-menu .el-menu-item.is-active')
    if (activeItem && indicatorRef.value) {
      gsap.to(indicatorRef.value, {
        y: activeItem.offsetTop + 8,
        height: activeItem.offsetHeight - 16,
        duration: 0.4,
        ease: 'power3.out'
      })
    }
  })
}

onMounted(() => {
  if (!sidebarRef.value) return

  ctx = gsap.context(() => {
    // Menu items stagger in
    gsap.from('.sidebar-menu .el-menu-item', {
      y: 12,
      opacity: 0,
      stagger: { each: 0.06, from: 'start' },
      duration: 0.4,
      ease: 'power3.out',
      delay: 0.15
    })

    // Initialize indicator
    nextTick(() => {
      const activeItem = document.querySelector('.sidebar-menu .el-menu-item.is-active')
      if (activeItem && indicatorRef.value) {
        gsap.set(indicatorRef.value, {
          y: activeItem.offsetTop + 8,
          height: activeItem.offsetHeight - 16,
          opacity: 1
        })
      }
    })

    // Header slide in
    gsap.from('.header', { y: -40, opacity: 0, duration: 0.5, ease: 'power3.out', delay: 0.1 })
  }, sidebarRef.value)
})

onUnmounted(() => { ctx?.revert() })
</script>

<style scoped>
.app-layout { height: 100vh; }

/* ===== Sidebar ===== */
.sidebar {
  background: var(--bg-elevated);
  overflow: hidden;
  position: relative;
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
}

.logo {
  height: 56px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 20px;
  border-bottom: 1px solid var(--border-subtle);
  flex-shrink: 0;
}

.logo-mark {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-text {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 0.5px;
}

.nav-wrapper { position: relative; padding: 12px 0; flex: 1; }

.nav-indicator {
  position: absolute;
  left: 0;
  width: 3px;
  background: var(--accent);
  border-radius: 0 3px 3px 0;
  opacity: 0;
  z-index: 2;
}

.sidebar-menu {
  border-right: none;
  background: transparent !important;
}

.sidebar-menu :deep(.el-menu-item) {
  height: 44px;
  line-height: 44px;
  margin: 2px 12px;
  border-radius: var(--radius-sm);
  transition: all var(--duration) var(--ease);
  font-size: 14px;
  font-weight: 500;
}

.sidebar-menu :deep(.el-menu-item:hover) {
  background: var(--accent-muted);
  color: var(--text-primary);
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background: var(--accent-muted) !important;
  color: var(--text-primary) !important;
  font-weight: 600;
}

.sidebar-menu :deep(.el-menu-item .el-icon) {
  font-size: 17px;
  margin-right: 10px;
}

/* Footer */
.sidebar-footer {
  padding: 16px 20px;
  border-top: 1px solid var(--border-subtle);
  flex-shrink: 0;
}

.footer-user {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--accent-muted);
  color: var(--accent);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  flex-shrink: 0;
}

.user-name {
  font-size: 13px;
  color: var(--text-secondary);
  font-weight: 500;
}

/* ===== Header ===== */
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--bg-elevated);
  border-bottom: 1px solid var(--border);
  padding: 0 28px;
  flex-shrink: 0;
}

.page-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 0;
}

.current-user {
  font-size: 13px;
  color: var(--text-secondary);
  font-weight: 500;
  margin-right: 12px;
}

.logout-btn {
  font-weight: 500;
  color: var(--text-muted) !important;
  font-size: 13px;
}
.logout-btn:hover {
  color: var(--status-error) !important;
}

/* ===== Content ===== */
.main-content {
  background: var(--bg);
  padding: 24px;
  overflow-y: auto;
}
</style>
