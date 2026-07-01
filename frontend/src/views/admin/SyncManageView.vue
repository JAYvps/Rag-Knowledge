<!-- ============ src/views/admin/SyncManageView.vue ============ -->
<!--
  Clean admin page:
  - Action bar with minimal buttons
  - Clean table with status indicators
  - No gradient icons, no glow effects
  - Consistent with new design system
-->
<template>
  <div class="sync-manage">
    <!-- Action bar -->
    <div class="action-bar">
      <div class="action-left">
        <h2 class="page-title">同步管理</h2>
        <el-tag type="info" size="small" class="count-tag">{{ repos.length }} 个知识库</el-tag>
      </div>
      <div class="action-right">
        <el-button @click="showAddDialog = true">
          <el-icon><Plus /></el-icon>
          添加知识库
        </el-button>
        <el-button
            type="primary"
            :loading="syncingAll"
            @click="handleSyncAll"
        >
          <el-icon><Refresh /></el-icon>
          全部同步
        </el-button>
      </div>
    </div>

    <!-- Repos table -->
    <div class="repos-section">
      <el-table :data="repos" v-loading="loading" class="repos-table">
        <el-table-column label="知识库" min-width="220">
          <template #default="{ row }">
            <div class="repo-info">
              <div class="repo-icon" :class="row.repoSource === 'OWN' ? 'repo-icon--own' : 'repo-icon--public'">
                {{ row.repoSource === 'OWN' ? '己' : '公' }}
              </div>
              <div>
                <div class="repo-name">{{ row.name }}</div>
                <div class="repo-meta">{{ row.namespace }}/{{ row.yuqueRepoSlug }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="来源" width="80" align="center">
          <template #default="{ row }">
            <span class="source-tag" :class="row.repoSource === 'OWN' ? 'source-tag--own' : 'source-tag--public'">
              {{ row.repoSource }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="同步状态" width="120" align="center">
          <template #default="{ row }">
            <span v-if="row.syncStatus === 0" class="status-badge status-badge--pending">
              <span class="status-dot"></span> 未同步
            </span>
            <span v-else-if="row.syncStatus === 1" class="status-badge status-badge--syncing">
              <span class="status-dot"></span> 同步中
            </span>
            <span v-else-if="row.syncStatus === 2" class="status-badge status-badge--done">
              <span class="status-dot"></span> 已完成
            </span>
            <el-tooltip v-else-if="row.syncStatus === 3" :content="row.lastSyncMsg || '未知错误'" placement="top">
              <span class="status-badge status-badge--error">
                <span class="status-dot"></span> 失败
              </span>
            </el-tooltip>
          </template>
        </el-table-column>

        <el-table-column label="文档数" width="80" align="center">
          <template #default="{ row }">
            <span class="number-cell">{{ row.docCount || 0 }}</span>
          </template>
        </el-table-column>

        <el-table-column label="分块数" width="80" align="center">
          <template #default="{ row }">
            <span class="number-cell">{{ row.chunkCount || 0 }}</span>
          </template>
        </el-table-column>

        <el-table-column label="最后同步" width="170" align="center">
          <template #default="{ row }">
            <span class="time-cell">{{ row.lastSyncAt ? formatTime(row.lastSyncAt) : '从未同步' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="同步结果" min-width="180">
          <template #default="{ row }">
            <span class="msg-cell">{{ row.lastSyncMsg || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <div class="action-btns">
              <el-button
                  type="primary"
                  text
                  size="small"
                  :loading="row._syncing"
                  @click="handleSyncOne(row)"
              >
                同步
              </el-button>
              <el-button
                  text
                  size="small"
                  @click="handleViewDocs(row)"
              >
                文档
              </el-button>
              <el-popconfirm
                  title="确定删除？所有文档向量和分块将被清除。"
                  confirm-button-text="删除"
                  cancel-button-text="取消"
                  @confirm="handleDelete(row.id)"
              >
                <template #reference>
                  <el-button type="danger" text size="small">删除</el-button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- Add dialog -->
    <el-dialog v-model="showAddDialog" title="添加语雀知识库" width="520px" class="add-dialog">
      <el-form :model="addForm" label-width="100px">
        <el-form-item label="namespace" required>
          <el-input v-model="addForm.namespace" placeholder="如: daluosishidabuguorenjiade" />
          <div class="form-tip">语雀用户名或组织名（URL中的路径）</div>
        </el-form-item>
        <el-form-item label="slug" required>
          <el-input v-model="addForm.slug" placeholder="如: hp14fu" />
          <div class="form-tip">知识库标识（URL中的路径）</div>
        </el-form-item>
        <el-form-item label="文章slug">
          <el-input v-model="addForm.articleSlug" placeholder="如: uwohuv6bh94xqcr7（选填）" />
          <div class="form-tip">不填则同步整个知识库，填写则只同步该篇文章</div>
        </el-form-item>
        <el-form-item label="显示名称">
          <el-input v-model="addForm.name" placeholder="可选，默认用知识库原名" />
        </el-form-item>
        <el-form-item>
          <div class="add-preview">
            完整路径：<code>{{ addForm.namespace || '...' }}/{{ addForm.slug || '...' }}{{ addForm.articleSlug ? '/' + addForm.articleSlug : '' }}</code>
            <span v-if="addForm.articleSlug" class="preview-mode">（单篇模式）</span>
            <span v-else class="preview-mode">（全库模式）</span>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" :loading="adding" @click="handleAddRepo">
          添加并同步
        </el-button>
      </template>
    </el-dialog>

    <!-- Docs drawer -->
    <el-drawer v-model="drawerVisible" :title="drawerTitle" size="55%">
      <el-table :data="drawerDocs" v-loading="drawerLoading">
        <el-table-column label="标题" prop="title" min-width="200" />
        <el-table-column label="字数" prop="wordCount" width="80" align="center" />
        <el-table-column label="分块" prop="chunkCount" width="80" align="center" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <span :class="row.syncStatus === 1 ? 'doc-status--ok' : 'doc-status--fail'">
              {{ row.syncStatus === 1 ? '正常' : '失败' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="同步时间" width="170" align="center">
          <template #default="{ row }">
            {{ row.yuqueUpdatedAt ? formatTime(row.yuqueUpdatedAt) : '-' }}
          </template>
        </el-table-column>
      </el-table>
      <div v-if="drawerDocs.length === 0 && !drawerLoading" class="empty-drawer">
        <el-icon :size="32" color="#d6d3d1"><Document /></el-icon>
        <p>暂无文档</p>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { getAllRepos, addRepo, syncRepo, syncAllRepos, getRepoDocs, deleteRepo } from '../../api/admin'
import { ElMessage } from 'element-plus'

const repos = ref([])
const loading = ref(false)
const syncingAll = ref(false)

const showAddDialog = ref(false)
const adding = ref(false)
const addForm = ref({ namespace: '', slug: '', articleSlug: '', name: '' })

const drawerVisible = ref(false)
const drawerTitle = ref('')
const drawerDocs = ref([])
const drawerLoading = ref(false)

let pollTimer = null

onMounted(async () => {
  await loadRepos()
  pollTimer = setInterval(async () => {
    const hasSyncing = repos.value.some(r => r.syncStatus === 1)
    if (hasSyncing) await loadRepos()
  }, 3000)
})

onUnmounted(() => { if (pollTimer) clearInterval(pollTimer) })

async function loadRepos() {
  loading.value = true
  try {
    const res = await getAllRepos()
    repos.value = (res.data || []).map(r => ({ ...r, _syncing: false }))
  } finally {
    loading.value = false
  }
}

async function handleAddRepo() {
  const { namespace, slug } = addForm.value
  if (!namespace || !slug) {
    ElMessage.warning('请填写 namespace 和 slug')
    return
  }
  adding.value = true
  try {
    await addRepo(addForm.value)
    ElMessage.success('添加成功，同步完成')
    showAddDialog.value = false
    addForm.value = { namespace: '', slug: '', articleSlug: '', name: '' }
    await loadRepos()
  } catch (e) {} finally {
    adding.value = false
  }
}

async function handleSyncOne(row) {
  row._syncing = true
  try {
    await syncRepo(row.id)
    ElMessage.success('同步完成')
    await loadRepos()
  } catch (e) {} finally {
    row._syncing = false
  }
}

async function handleSyncAll() {
  syncingAll.value = true
  try {
    await syncAllRepos()
    ElMessage.success('全部同步完成')
    await loadRepos()
  } catch (e) {} finally {
    syncingAll.value = false
  }
}

async function handleViewDocs(row) {
  drawerTitle.value = row.name + ' - 文档列表'
  drawerVisible.value = true
  drawerLoading.value = true
  try {
    const res = await getRepoDocs(row.id)
    drawerDocs.value = res.data || []
  } finally {
    drawerLoading.value = false
  }
}

async function handleDelete(repoId) {
  try {
    await deleteRepo(repoId)
    ElMessage.success('已删除')
    await loadRepos()
  } catch (e) {}
}

function formatTime(time) {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}
</script>

<style scoped>
.sync-manage {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* Action bar */
.action-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--bg-elevated);
  border: 1px solid var(--border);
  border-radius: var(--radius-xl);
  padding: 20px 24px;
}

.action-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.page-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
}

.count-tag {
  font-weight: 600;
  font-size: 11px;
}

.action-right {
  display: flex;
  gap: 8px;
}

/* Repos section */
.repos-section {
  background: var(--bg-elevated);
  border: 1px solid var(--border);
  border-radius: var(--radius-xl);
  padding: 20px 24px;
}

.repo-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.repo-icon {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
}

.repo-icon--own {
  background: var(--accent);
}

.repo-icon--public {
  background: var(--text-muted);
}

.repo-name {
  font-size: 13px;
  color: var(--text-primary);
  font-weight: 600;
}

.repo-meta {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 2px;
}

.source-tag {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 4px;
}

.source-tag--own {
  background: var(--accent-muted);
  color: var(--accent);
}

.source-tag--public {
  background: var(--bg-muted);
  color: var(--text-muted);
}

/* Status badges */
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 500;
  padding: 2px 10px;
  border-radius: 20px;
}

.status-badge--pending { color: var(--text-muted); background: var(--bg-muted); }
.status-badge--syncing { color: #d97706; background: rgba(245, 158, 11, 0.08); }
.status-badge--done { color: #16a34a; background: rgba(34, 197, 94, 0.08); }
.status-badge--error { color: #dc2626; background: rgba(239, 68, 68, 0.08); cursor: help; }

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.status-badge--syncing .status-dot {
  animation: pulse-dot 1.5s ease-in-out infinite;
}

@keyframes pulse-dot {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
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

.msg-cell {
  font-size: 12px;
  color: var(--text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.action-btns {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

/* Form tips */
.form-tip {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 4px;
}

.add-preview {
  color: var(--text-secondary);
  font-size: 13px;
}

.add-preview code {
  background: var(--bg-muted);
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 600;
  color: var(--accent);
  font-family: var(--font-mono);
  font-size: 12px;
}

.preview-mode {
  margin-left: 8px;
  font-size: 12px;
  color: var(--accent);
  font-weight: 600;
}

/* Drawer empty */
.empty-drawer {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 20px;
  gap: 12px;
}

.empty-drawer p {
  font-size: 14px;
  color: var(--text-muted);
}

.doc-status--ok {
  color: #16a34a;
  font-weight: 600;
  font-size: 12px;
}

.doc-status--fail {
  color: #dc2626;
  font-weight: 600;
  font-size: 12px;
}
</style>
