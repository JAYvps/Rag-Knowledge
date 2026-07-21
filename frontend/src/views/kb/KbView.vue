<!-- ============ src/views/kb/KbView.vue ============ -->
<!--
  Clean knowledge base browser:
  - Two-panel layout with subtle borders
  - No purple accents, no glow effects
  - Emerald accent for active states
  - Clean empty state with icon
-->
<template>
  <div class="kb-container" ref="containerRef">
    <!-- Left: Knowledge base list -->
    <div class="kb-sidebar">
      <div class="kb-sidebar-header">
        <div class="header-top">
          <h2 class="header-title">知识库</h2>
          <el-tag size="small" type="info" class="count-tag">{{ repos.length }}</el-tag>
        </div>
      </div>

      <div class="kb-list">
        <!-- 语雀知识库 -->
        <div class="kb-section-header">
          <span class="section-label">语雀知识库</span>
        </div>
        <div
            v-for="repo in repos"
            :key="repo.id"
            :class="['kb-item', { active: selectedType === 'yuque' && selectedRepo?.id === repo.id }]"
            @click="selectRepo(repo, 'yuque')"
        >
          <div class="kb-item-icon kb-item-icon--yuque">
            <el-icon :size="16"><Collection /></el-icon>
          </div>
          <div class="kb-item-info">
            <div class="kb-item-name">{{ repo.name }}</div>
            <div class="kb-item-meta">
              <span>{{ repo.docCount || 0 }} 篇文档</span>
              <span v-if="repo.syncStatus === 2" class="status-tag status-tag--success">已同步</span>
              <span v-else-if="repo.syncStatus === 1" class="status-tag status-tag--warning">同步中</span>
              <span v-else-if="repo.syncStatus === 3" class="status-tag status-tag--error">失败</span>
              <span v-else class="status-tag">未同步</span>
            </div>
          </div>
        </div>

        <!-- 全局文档 -->
        <template v-if="globalDocs.length > 0">
          <div class="kb-section-header" style="margin-top: 12px;">
            <span class="section-label">全局文档</span>
            <el-tag size="small" type="success">{{ globalDocs.length }}</el-tag>
          </div>
          <div
              :class="['kb-item', { active: selectedType === 'global' }]"
              @click="selectRepo({ id: 'global', name: '全局文档' }, 'global')"
          >
            <div class="kb-item-icon kb-item-icon--global">
              <el-icon :size="16"><Share /></el-icon>
            </div>
            <div class="kb-item-info">
              <div class="kb-item-name">全局文档</div>
              <div class="kb-item-meta">
                <span>{{ globalDocs.length }} 篇文档</span>
                <span class="status-tag status-tag--success">全员可搜</span>
              </div>
            </div>
          </div>
        </template>

        <div v-if="repos.length === 0 && globalDocs.length === 0 && !loading" class="empty-sidebar">
          <el-icon :size="32" color="#d6d3d1"><Collection /></el-icon>
          <p>暂无知识库</p>
        </div>
      </div>
    </div>

    <!-- Right: Document list -->
    <div class="kb-main" ref="kbMainRef">
      <template v-if="selectedRepo">
        <div class="kb-main-header">
          <div class="main-header-content">
            <h3 class="repo-title">{{ selectedRepo.name }}</h3>
            <p v-if="selectedRepo.description" class="repo-desc">{{ selectedRepo.description }}</p>
          </div>
          <div class="header-stats">
            <span class="stat-value">{{ docs.length }}</span>
            <span class="stat-label">文档</span>
          </div>
        </div>

        <el-table :data="docs" v-loading="docsLoading" class="doc-table">
          <el-table-column label="文档标题" min-width="300">
            <template #default="{ row }">
              <div class="doc-title">
                <div class="doc-icon" :class="`doc-icon--${row.sourceType}`">
                  <el-icon :size="14"><Document /></el-icon>
                </div>
                <span>{{ row.title }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="来源" width="120" align="center">
            <template #default="{ row }">
              <el-tag
                :type="row.sourceType === 'yuque' ? 'primary' : 'success'"
                size="small"
                effect="plain"
              >
                {{ row.sourceLabel }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="字数" width="100" align="center">
            <template #default="{ row }">
              <span class="number-cell">{{ row.wordCount ? row.wordCount.toLocaleString() : '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="分块" width="80" align="center">
            <template #default="{ row }">
              <span class="number-cell">{{ row.chunkCount || 0 }}</span>
            </template>
          </el-table-column>
          <el-table-column label="更新时间" width="180" align="center">
            <template #default="{ row }">
              <span class="time-cell">{{ formatTime(row.updatedAt) }}</span>
            </template>
          </el-table-column>
        </el-table>

        <div v-if="docs.length === 0 && !docsLoading" class="empty-docs">
          <el-icon :size="40" color="#d6d3d1"><Document /></el-icon>
          <p>该知识库暂无文档</p>
        </div>
      </template>

      <div v-else class="placeholder">
        <div class="placeholder-icon">
          <el-icon :size="48" color="#d6d3d1"><Collection /></el-icon>
        </div>
        <p class="placeholder-text">请从左侧选择一个知识库</p>
        <p class="placeholder-hint">选择后将展示文档列表</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { getRepoList, getDocList, getGlobalDocs } from '../../api/kb'
import gsap from 'gsap'

const route = useRoute()
const repos = ref([])
const globalDocs = ref([])
const selectedRepo = ref(null)
const selectedType = ref(null) // 'yuque' or 'global'
const docs = ref([])
const loading = ref(false)
const docsLoading = ref(false)
const containerRef = ref(null)
let ctx

onMounted(async () => {
  loading.value = true
  try {
    const [reposRes, globalRes] = await Promise.all([
      getRepoList(),
      getGlobalDocs()
    ])
    repos.value = reposRes.data || []
    globalDocs.value = globalRes.data || []
    if (route.params.repoId) {
      const repo = repos.value.find(r => r.id === Number(route.params.repoId))
      if (repo) selectRepo(repo, 'yuque')
    }
  } finally {
    loading.value = false
  }

  if (!containerRef.value) return
  ctx = gsap.context(() => {
    gsap.from('.kb-sidebar-header', { y: -12, opacity: 0, duration: 0.4, ease: 'power3.out' })
    gsap.from('.kb-item', {
      y: 12,
      opacity: 0,
      stagger: { each: 0.05, from: 'start' },
      duration: 0.35,
      ease: 'power3.out',
      delay: 0.15
    })
    gsap.from('.placeholder-icon', { scale: 0.8, opacity: 0, duration: 0.6, ease: 'back.out(2)', delay: 0.3 })
    gsap.from('.placeholder-text', { y: 12, opacity: 0, duration: 0.4, delay: 0.5 })
    gsap.from('.placeholder-hint', { y: 8, opacity: 0, duration: 0.4, delay: 0.6 })
  }, containerRef.value)
})

onUnmounted(() => { ctx?.revert() })

async function selectRepo(repo, type) {
  selectedRepo.value = repo
  selectedType.value = type
  docsLoading.value = true
  try {
    if (type === 'yuque') {
      const res = await getDocList(repo.id)
      docs.value = (res.data || []).map(d => ({
        ...d,
        sourceType: 'yuque',
        sourceLabel: '语雀同步'
      }))
    } else if (type === 'global') {
      docs.value = globalDocs.value.map(d => ({
        id: d.id,
        title: d.title || d.fileName,
        wordCount: d.wordCount,
        chunkCount: d.chunkCount,
        updatedAt: d.updatedAt,
        sourceType: 'global',
        sourceLabel: '管理员上传'
      }))
    }
    nextTick(() => {
      gsap.from('.kb-main-header', { y: -12, opacity: 0, duration: 0.4, ease: 'power3.out' })
      if (docs.value.length > 0) {
        gsap.from('.el-table__row', {
          y: 8,
          opacity: 0,
          stagger: 0.03,
          duration: 0.3,
          ease: 'power3.out',
          delay: 0.1
        })
      }
    })
  } finally {
    docsLoading.value = false
  }
}

function formatTime(time) {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}
</script>

<style scoped>
.kb-container {
  display: flex;
  height: calc(100vh - 120px);
  background: var(--bg-elevated);
  border-radius: var(--radius-xl);
  overflow: hidden;
  border: 1px solid var(--border);
}

/* ===== Left sidebar ===== */
.kb-sidebar {
  width: 280px;
  background: var(--bg-elevated);
  border-right: 1px solid var(--border-subtle);
  display: flex;
  flex-direction: column;
}

.kb-sidebar-header {
  padding: 20px 20px 16px;
  border-bottom: 1px solid var(--border-subtle);
}

.header-top {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
}

.count-tag {
  font-weight: 600;
  font-size: 11px;
}

.kb-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 12px;
}

.kb-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 8px 4px;
}

.section-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.kb-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--duration) var(--ease);
  margin-bottom: 2px;
}

.kb-item:hover {
  background: var(--bg-muted);
}

.kb-item.active {
  background: var(--accent-muted);
}

.kb-item-icon {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  background: var(--bg-muted);
  color: var(--text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all var(--duration) var(--ease);
}

.kb-item-icon--yuque {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
}

.kb-item-icon--global {
  background: rgba(34, 197, 94, 0.1);
  color: #16a34a;
}

.kb-item.active .kb-item-icon {
  background: var(--accent-muted);
  color: var(--accent);
}

.kb-item-info {
  flex: 1;
  min-width: 0;
}

.kb-item-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.kb-item-meta {
  font-size: 12px;
  color: var(--text-muted);
  display: flex;
  align-items: center;
  gap: 6px;
}

.status-tag {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 4px;
  font-weight: 600;
}

.status-tag--success {
  background: rgba(34, 197, 94, 0.1);
  color: #16a34a;
}

.status-tag--warning {
  background: rgba(245, 158, 11, 0.1);
  color: #d97706;
}

.status-tag--error {
  background: rgba(239, 68, 68, 0.1);
  color: #dc2626;
}

.empty-sidebar {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  gap: 8px;
}

.empty-sidebar p {
  font-size: 13px;
  color: var(--text-muted);
}

/* ===== Right main ===== */
.kb-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--bg-elevated);
}

.kb-main-header {
  padding: 20px 24px;
  border-bottom: 1px solid var(--border-subtle);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.repo-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 2px;
}

.repo-desc {
  color: var(--text-muted);
  font-size: 13px;
}

.header-stats {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.stat-value {
  font-size: 24px;
  font-weight: 800;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}

.stat-label {
  font-size: 11px;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  font-weight: 500;
}

.doc-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.doc-icon {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  background: var(--bg-muted);
  color: var(--text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.doc-icon--yuque {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
}

.doc-icon--global {
  background: rgba(34, 197, 94, 0.1);
  color: #16a34a;
}

.number-cell {
  font-variant-numeric: tabular-nums;
  font-weight: 500;
  color: var(--text-primary);
  font-size: 13px;
}

.time-cell {
  font-size: 13px;
  color: var(--text-muted);
}

.empty-docs {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  gap: 12px;
}

.empty-docs p {
  font-size: 14px;
  color: var(--text-muted);
}

.placeholder {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.placeholder-icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: var(--bg-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8px;
}

.placeholder-text {
  font-size: 15px;
  color: var(--text-secondary);
  font-weight: 600;
}

.placeholder-hint {
  font-size: 13px;
  color: var(--text-muted);
}

@media (max-width: 768px) {
  .kb-container {
    flex-direction: column;
    height: auto;
  }
  .kb-sidebar {
    width: 100%;
    max-height: 300px;
    border-right: none;
    border-bottom: 1px solid var(--border-subtle);
  }
}
</style>
