<!-- ============ src/views/doc/MyDocsView.vue ============ -->
<!--
  Clean document management:
  - Minimal upload area with dashed border
  - Status dots with subtle pulse
  - No gradient icons, no glow effects
  - Clean table with muted accents
-->
<template>
  <el-dialog v-model="visible" title="预览" width="80%" destroy-on-close>
    <div v-loading="previewLoading">
      <vue-pdf-embed v-show="fileType === 'pdf'" :source="previewUrl" style="height: 70vh;" />
      <div v-show="fileType === 'docx'" ref="docxRef" style="height: 70vh; overflow: auto;"></div>
      <div v-show="['xlsx', 'xls'].includes(fileType)" ref="excelRef" style="height: 70vh; overflow: auto;"></div>
      <pre v-show="['txt', 'md'].includes(fileType)" style="height: 70vh; overflow: auto;">{{ textContent }}</pre>
    </div>
  </el-dialog>

  <div class="my-docs">
    <!-- Upload area -->
    <div class="upload-section">
      <el-upload
        ref="uploadRef"
        drag
        :auto-upload="false"
        :on-change="handleFileChange"
        :before-upload="beforeUpload"
        accept=".pdf,.txt,.md,.docx,.xlsx"
        :limit="1"
        class="upload-zone"
      >
        <div class="upload-content">
          <div class="upload-icon">
            <el-icon :size="28"><UploadFilled /></el-icon>
          </div>
          <p class="upload-text">拖拽文件到此处，或<em>点击上传</em></p>
          <p class="upload-tip">支持 PDF、TXT、MD、DOCX、XLSX 格式，单个文件最大 10MB</p>
        </div>
      </el-upload>

      <div class="upload-actions">
        <el-input
          v-model="uploadTitle"
          placeholder="文档标题（可选，默认用文件名）"
          style="width: 280px"
        />
        <el-button type="primary" :loading="uploading" @click="handleUpload">
          开始上传
        </el-button>
      </div>
    </div>

    <!-- Document list -->
    <div class="docs-section">
      <div class="section-header">
        <div class="header-left">
          <h2 class="section-title">我的文档</h2>
          <el-tag type="info" size="small" class="count-tag">{{ docs.length }} 篇</el-tag>
        </div>
      </div>

      <el-table :data="docs" v-loading="loading" class="docs-table">
        <el-table-column label="文件名" min-width="200">
          <template #default="{ row }">
            <div class="file-info">
              <div class="file-icon" :class="`file-icon--${row.fileType}`">
                {{ row.fileType?.toUpperCase()?.charAt(0) }}
              </div>
              <div class="file-details">
                <div class="file-name">{{ row.title }}</div>
                <div class="file-meta">{{ row.fileName }} · {{ formatSize(row.fileSize) }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="类型" width="80" align="center">
          <template #default="{ row }">
            <span class="file-type">{{ row.fileType?.toUpperCase() }}</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <div class="status-cell">
              <span v-if="row.status === 0" class="status-badge status-badge--pending">
                <span class="status-dot"></span> 待处理
              </span>
              <span v-else-if="row.status === 1" class="status-badge status-badge--processing">
                <span class="status-dot"></span> 处理中
              </span>
              <span v-else-if="row.status === 2" class="status-badge status-badge--ready">
                <span class="status-dot"></span> 就绪
              </span>
              <el-tooltip v-else-if="row.status === 3" :content="row.errorMsg || '未知错误'" placement="top">
                <span class="status-badge status-badge--failed">
                  <span class="status-dot"></span> 失败
                </span>
              </el-tooltip>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="分块" width="80" align="center">
          <template #default="{ row }">
            <span class="number-cell">{{ row.status === 2 ? row.chunkCount : '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="字数" width="100" align="center">
          <template #default="{ row }">
            <span class="number-cell">{{ row.status === 2 && row.wordCount ? row.wordCount.toLocaleString() : '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="上传时间" width="170" align="center">
          <template #default="{ row }">
            <span class="time-cell">{{ formatTime(row.createdAt) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <div style="display: flex; flex-direction: column; align-items: center; gap: 4px;">
              <el-button type="primary" text size="small" @click="handlePreview(row.id, row.fileName)">
                预览
              </el-button>
              <el-popconfirm
                  title="确定删除此文档？删除后向量数据也会被清除。"
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


      <div v-if="docs.length === 0 && !loading" class="empty-docs">
        <el-icon :size="40" color="#d6d3d1"><Document /></el-icon>
        <p>暂无文档，上传一个试试</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, onUnmounted } from 'vue'
import { uploadDoc, getDocList, deleteDoc } from '../../api/doc'
import { ElMessage } from 'element-plus'
import VuePdfEmbed from 'vue-pdf-embed'
import { renderAsync } from 'docx-preview'
import * as XLSX from 'xlsx'

const docs = ref([])
const loading = ref(false)
const uploading = ref(false)
const uploadTitle = ref('')
const selectedFile = ref(null)
const uploadRef = ref(null)
let pollTimer = null
const visible = ref(false)
const fileType = ref('')
const textContent = ref('')
const previewLoading = ref(false)
const docxRef = ref(null)
const excelRef = ref(null)
const previewUrl = ref('')
const handlePreview = async (id, fileName) => {
  const ext = fileName.split('.').pop().toLowerCase()
  const token = localStorage.getItem('token')
  previewLoading.value = true

  try {
    if (['xlsx', 'xls'].includes(ext)) {
      const res = await fetch(`/api/doc/preview?id=${id}`, {
        headers: { 'Authorization': `Bearer ${token}` }
      })
      const buffer = await res.arrayBuffer()
      const workbook = XLSX.read(buffer, { type: 'array' })
      const sheet = workbook.Sheets[workbook.SheetNames[0]]
      const html = XLSX.utils.sheet_to_html(sheet)
      fileType.value = ext
      visible.value = true
      await nextTick()
      excelRef.value.innerHTML = html
    } else if (ext === 'docx') {
      const res = await fetch(`/api/doc/preview?id=${id}`, {
        headers: { 'Authorization': `Bearer ${token}` }
      })
      const blob = await res.blob()
      fileType.value = ext
      visible.value = true
      await nextTick()
      await renderAsync(blob, docxRef.value)
    } else if (ext === 'pdf') {
      const res = await fetch(`/api/doc/preview?id=${id}`, {
        headers: { 'Authorization': `Bearer ${token}` }
      })
      const blob = await res.blob()
      fileType.value = ext
      previewUrl.value = URL.createObjectURL(blob)
      visible.value = true
    } else if (['txt', 'md'].includes(ext)) {
      const res = await fetch(`/api/doc/preview?id=${id}`, {
        headers: { 'Authorization': `Bearer ${token}` }
      })
      textContent.value = await res.text()
      fileType.value = ext
      visible.value = true
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('预览失败：' + e.message)
  } finally {
    previewLoading.value = false
  }
}

onMounted(async () => {
  await loadDocs()

  pollTimer = setInterval(async () => {
    const hasProcessing = docs.value.some(d => d.status === 0 || d.status === 1)
    if (hasProcessing) await loadDocs()
  }, 3000)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})

async function loadDocs() {
  loading.value = true
  try {
    const res = await getDocList()
    docs.value = res.data || []
  } finally {
    loading.value = false
  }
}

function handleFileChange(file) { selectedFile.value = file.raw }

function beforeUpload(file) {
  const ext = file.name.split('.').pop().toLowerCase()
  if (!['pdf', 'txt', 'md', 'docx', 'xlsx'].includes(ext)) {
    ElMessage.error('仅支持 PDF、TXT、MD、DOCX、XLSX 格式')
    return false
  }
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过10MB')
    return false
  }
  return true
}

async function handleUpload() {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }
  uploading.value = true
  try {
    await uploadDoc(selectedFile.value, uploadTitle.value)
    ElMessage.success('上传成功，正在处理...')
    uploadTitle.value = ''
    selectedFile.value = null
    if (uploadRef.value) uploadRef.value.clearFiles()
    await loadDocs()
  } catch (e) {} finally {
    uploading.value = false
  }
}

async function handleDelete(docId) {
  try {
    await deleteDoc(docId)
    ElMessage.success('已删除')
    await loadDocs()
  } catch (e) {}
}

function formatSize(bytes) {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function formatTime(time) {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

</script>

<style scoped>
.my-docs {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* ===== Upload section ===== */
.upload-section {
  background: var(--bg-elevated);
  border: 1px solid var(--border);
  border-radius: var(--radius-xl);
  padding: 24px;
}

.upload-zone :deep(.el-upload-dragger) {
  padding: 32px;
  border-radius: var(--radius-md);
  border: 2px dashed var(--border);
  background: var(--bg-muted);
  transition: all var(--duration) var(--ease);
}

.upload-zone :deep(.el-upload-dragger):hover {
  border-color: var(--accent-border);
  background: var(--accent-muted);
}

.upload-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.upload-icon {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: var(--accent-muted);
  color: var(--accent);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 4px;
}

.upload-text {
  color: var(--text-secondary);
  font-size: 14px;
}

.upload-text em {
  color: var(--accent);
  font-style: normal;
  font-weight: 600;
}

.upload-tip {
  color: var(--text-muted);
  font-size: 12px;
}

.upload-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
}

/* ===== Docs section ===== */
.docs-section {
  background: var(--bg-elevated);
  border: 1px solid var(--border);
  border-radius: var(--radius-xl);
  padding: 24px;
}

.section-header {
  margin-bottom: 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.section-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
}

.count-tag {
  font-weight: 600;
  font-size: 11px;
}

/* File info */
.file-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.file-icon {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  background: var(--bg-muted);
  color: var(--text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
  transition: transform 0.2s var(--ease);
}

.file-info:hover .file-icon {
  transform: scale(1.05);
}

.file-icon--pdf { background: rgba(239, 68, 68, 0.08); color: #dc2626; }
.file-icon--txt { background: rgba(100, 116, 139, 0.08); color: #64748b; }
.file-icon--md { background: rgba(34, 197, 94, 0.08); color: #16a34a; }
.file-icon--docx { background: rgba(59, 130, 246, 0.08); color: #2563eb; }
.file-icon--xlsx { background: rgba(34, 197, 94, 0.08); color: #16a34a; }

.file-details {
  flex: 1;
  min-width: 0;
}

.file-name {
  font-size: 13px;
  color: var(--text-primary);
  font-weight: 600;
}

.file-meta {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 2px;
}

.file-type {
  font-size: 11px;
  font-weight: 600;
  color: var(--text-muted);
  background: var(--bg-muted);
  padding: 2px 8px;
  border-radius: 4px;
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
.status-badge--processing { color: #d97706; background: rgba(245, 158, 11, 0.08); }
.status-badge--ready { color: #16a34a; background: rgba(34, 197, 94, 0.08); }
.status-badge--failed { color: #dc2626; background: rgba(239, 68, 68, 0.08); cursor: help; }

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.status-badge--processing .status-dot {
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

.delete-btn {
  font-size: 12px;
}

.empty-docs {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 20px;
  gap: 12px;
}

.empty-docs p {
  font-size: 14px;
  color: var(--text-muted);
}
</style>
