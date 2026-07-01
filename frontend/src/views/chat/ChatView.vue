<!-- ============ src/views/chat/ChatView.vue ============ -->
<!--
  Clean chat interface:
  - Muted conversation sidebar
  - Clean message bubbles (no neon blue)
  - Emerald accent for user messages
  - Subtle reference cards
  - Typing cursor with clean animation
-->
<template>
  <div class="chat-container">
    <!-- Left: Conversation list -->
    <div class="chat-sidebar">
      <div class="sidebar-header">
        <el-button type="primary" @click="handleNewChat" class="new-chat-btn">
          <el-icon><Plus /></el-icon>
          新对话
        </el-button>
      </div>

      <div class="conversation-list">
        <div
            v-for="conv in conversations"
            :key="conv.id"
            :class="['conv-item', { active: currentConvId === conv.id }]"
            @click="selectConversation(conv.id)"
        >
          <el-icon class="conv-icon" :size="14"><ChatDotRound /></el-icon>
          <span class="conv-title">{{ conv.title }}</span>
          <el-icon
              class="conv-delete"
              :size="12"
              @click.stop="handleDeleteConv(conv.id)"
          >
            <Close />
          </el-icon>
        </div>

        <div v-if="conversations.length === 0" class="empty-sidebar">
          <el-icon :size="24" color="#d6d3d1"><ChatDotRound /></el-icon>
          <p>暂无对话</p>
        </div>
      </div>
    </div>

    <!-- Right: Chat area -->
    <div class="chat-main">
      <div class="message-list" ref="messageListRef">
        <!-- Empty state -->
        <div v-if="messages.length === 0 && !streaming" class="empty-chat">
          <div class="empty-icon">
            <el-icon :size="32" color="#a8a29e"><ChatDotRound /></el-icon>
          </div>
          <p class="empty-text">开始你的第一个问题吧</p>
          <p class="empty-hint">基于全部知识库进行智能检索和回答</p>
        </div>

        <!-- Messages -->
        <div
            v-for="(msg, index) in messages"
            :key="index"
            :class="['message', msg.role]"
        >
          <div class="message-avatar" :class="`avatar--${msg.role}`">
            <el-icon v-if="msg.role === 'user'" :size="14"><User /></el-icon>
            <el-icon v-else :size="14"><Monitor /></el-icon>
          </div>

          <div class="message-body">
            <div
                class="message-content"
                v-html="renderMarkdown(msg.content)"
            ></div>

            <!-- References -->
            <div
                v-if="msg.role === 'assistant' && msg.references && msg.references.length"
                class="references"
            >
              <el-collapse>
                <el-collapse-item>
                  <template #title>
                    <span class="ref-header">
                      <el-icon :size="12"><Link /></el-icon>
                      引用来源 ({{ filteredReferences(msg.references).length }})
                    </span>
                  </template>
                  <div v-for="ref in filteredReferences(msg.references)" :key="ref.index" class="ref-item">
                    <span class="ref-index">[{{ ref.index }}]</span>
                    <span class="ref-source">{{ ref.sourceLabel }}</span>
                    <span class="ref-doc">{{ ref.displayTitle || ref.docTitle }}</span>
                    <span class="ref-score">{{ (ref.score * 100).toFixed(0) }}%</span>
                  </div>
                </el-collapse-item>
              </el-collapse>
            </div>
          </div>
        </div>

        <!-- Streaming message -->
        <div v-if="streaming" class="message assistant">
          <div class="message-avatar avatar--assistant">
            <el-icon :size="14"><Monitor /></el-icon>
          </div>
          <div class="message-body">
            <div
                class="message-content"
                v-html="renderMarkdown(streamingContent)"
            ></div>
            <span class="typing-cursor"></span>
          </div>
        </div>
      </div>

      <!-- Input area -->
      <div class="input-area">
        <el-input
            v-model="inputText"
            type="textarea"
            :autosize="{ minRows: 1, maxRows: 4 }"
            placeholder="输入问题... (Enter 发送, Shift+Enter 换行)"
            @keydown="handleKeydown"
            :disabled="streaming"
            resize="none"
            class="chat-input"
        />
        <div class="input-actions">
          <span class="input-tip" v-if="streaming">AI 正在回答中...</span>
          <span class="input-tip" v-else>按 Enter 发送</span>
          <el-button
              type="primary"
              :loading="streaming"
              @click="sendMessage"
              :disabled="!inputText.trim()"
              class="send-btn"
          >
            发送
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { getConversations, getHistory, deleteConversation, askQuestion } from '../../api/chat'
import { ElMessage, ElMessageBox } from 'element-plus'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'

const md = new MarkdownIt({
  highlight(str, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return hljs.highlight(str, { language: lang }).value
      } catch (_) {}
    }
    return md.utils.escapeHtml(str)
  }
})

function renderMarkdown(text) {
  if (!text) return ''
  return md.render(text)
}

function filteredReferences(refs) {
  if (!refs) return []
  const filtered = refs.filter(ref => ref.score * 100 > 40)
  const docMap = new Map()

  filtered.forEach(ref => {
    const docTitle = ref.docTitle
    if (!docMap.has(docTitle)) {
      docMap.set(docTitle, { ref, chunkCount: 1, maxScore: ref.score })
    } else {
      const existing = docMap.get(docTitle)
      existing.chunkCount++
      if (ref.score > existing.maxScore) {
        existing.ref = ref
        existing.maxScore = ref.score
      }
    }
  })

  return Array.from(docMap.entries())
    .map(([docTitle, data], index) => ({
      ...data.ref,
      index: index + 1,
      chunkCount: data.chunkCount,
      displayTitle: data.chunkCount > 1 ? ` (${data.chunkCount}个相关分片)` : docTitle
    }))
    .sort((a, b) => b.maxScore - a.maxScore)
}

const conversations = ref([])
const currentConvId = ref(null)
const messages = ref([])
const inputText = ref('')
const streaming = ref(false)
const streamingContent = ref('')
const messageListRef = ref(null)
let currentAbort = null
let savedRefs = null

onMounted(() => { loadConversations() })
onUnmounted(() => { if (currentAbort) currentAbort.abort() })

async function loadConversations() {
  try {
    const res = await getConversations()
    conversations.value = res.data || []
  } catch (_) {}
}

async function selectConversation(convId) {
  if (streaming.value) return
  currentConvId.value = convId
  messages.value = []
  try {
    const res = await getHistory(convId)
    messages.value = res.data || []
    scrollToBottom()
  } catch (_) {
    ElMessage.error('加载历史失败')
  }
}

function handleNewChat() {
  if (streaming.value) return
  currentConvId.value = null
  messages.value = []
  inputText.value = ''
}

async function handleDeleteConv(convId) {
  if (streaming.value) return
  await ElMessageBox.confirm('确定删除此对话？', '提示')
  try {
    await deleteConversation(convId)
    if (currentConvId.value === convId) {
      currentConvId.value = null
      messages.value = []
    }
    loadConversations()
    ElMessage.success('已删除')
  } catch (_) {}
}

function sendMessage() {
  const question = inputText.value.trim()
  if (!question || streaming.value) return

  messages.value.push({ role: 'user', content: question, references: [] })
  inputText.value = ''
  scrollToBottom()

  streaming.value = true
  streamingContent.value = ''
  savedRefs = null

  currentAbort = askQuestion(
    { question, conversationId: currentConvId.value },
    {
      onReferences(refs) { savedRefs = refs },
      onContent(chunk) {
        streamingContent.value += (typeof chunk === 'string' ? chunk : String(chunk))
        scrollToBottom()
      },
      onDone(data) {
        if (streamingContent.value) {
          messages.value.push({
            role: 'assistant',
            content: streamingContent.value,
            references: savedRefs || []
          })
        }
        streaming.value = false
        streamingContent.value = ''
        if (data && data.conversationId) currentConvId.value = data.conversationId
        setTimeout(() => loadConversations(), 500)
        scrollToBottom()
      },
      onError: (err) => {
        if (streamingContent.value) {
          messages.value.push({
            role: 'assistant',
            content: streamingContent.value + '\n\n(回答被中断)',
            references: savedRefs || []
          })
        }
        streaming.value = false
        streamingContent.value = ''
        savedRefs = null
        ElMessage.error('问答失败: ' + (typeof err === 'string' ? err : '请重试'))
      }
    }
  )
}

function handleKeydown(e) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

function scrollToBottom() {
  nextTick(() => {
    const el = messageListRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}
</script>

<style scoped>
.chat-container {
  display: flex;
  height: calc(100vh - 120px);
  background: var(--bg-elevated);
  border-radius: var(--radius-xl);
  overflow: hidden;
  border: 1px solid var(--border);
}

/* ===== Sidebar ===== */
.chat-sidebar {
  width: 260px;
  min-width: 260px;
  border-right: 1px solid var(--border-subtle);
  display: flex;
  flex-direction: column;
  background: var(--bg-elevated);
}

.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid var(--border-subtle);
}

.new-chat-btn {
  width: 100%;
  border-radius: var(--radius-sm) !important;
  font-weight: 600;
  font-size: 13px;
  height: 36px;
}

.conversation-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.conv-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--duration) var(--ease);
  margin-bottom: 2px;
}

.conv-item:hover {
  background: var(--bg-muted);
}

.conv-item.active {
  background: var(--accent-muted);
}

.conv-icon {
  color: var(--text-muted);
  flex-shrink: 0;
}

.conv-title {
  flex: 1;
  font-size: 13px;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 500;
}

.conv-delete {
  opacity: 0;
  color: var(--text-muted);
  transition: opacity var(--duration) var(--ease);
  cursor: pointer;
}

.conv-item:hover .conv-delete {
  opacity: 1;
}

.conv-delete:hover {
  color: var(--status-error);
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

/* ===== Chat main ===== */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

/* Empty chat */
.empty-chat {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 12px;
}

.empty-icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: var(--bg-muted);
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-text {
  font-size: 15px;
  color: var(--text-secondary);
  font-weight: 600;
}

.empty-hint {
  font-size: 13px;
  color: var(--text-muted);
}

/* Messages */
.message {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  align-items: flex-start;
}

.message.user {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #fff;
}

.avatar--user {
  background: var(--accent);
}

.avatar--assistant {
  background: var(--text-muted);
}

.message-body {
  max-width: 72%;
  min-width: 60px;
}

.message-content {
  padding: 12px 16px;
  border-radius: var(--radius-md);
  font-size: 14px;
  line-height: 1.7;
  word-break: break-word;
}

.message.user .message-content {
  background: var(--accent);
  color: #fff;
  border-top-right-radius: 4px;
}

.message.assistant .message-content {
  background: var(--bg-muted);
  color: var(--text-primary);
  border-top-left-radius: 4px;
}

/* Markdown styles in assistant messages */
.message.assistant .message-content :deep(h1),
.message.assistant .message-content :deep(h2),
.message.assistant .message-content :deep(h3) {
  margin: 12px 0 8px 0;
  font-weight: 600;
  color: var(--text-primary);
}

.message.assistant .message-content :deep(pre) {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 12px 16px;
  border-radius: var(--radius-sm);
  overflow-x: auto;
  font-size: 13px;
  margin: 8px 0;
}

.message.assistant .message-content :deep(code) {
  font-family: var(--font-mono);
  font-size: 13px;
}

.message.assistant .message-content :deep(p) {
  margin: 6px 0;
}

.message.assistant .message-content :deep(ul),
.message.assistant .message-content :deep(ol) {
  padding-left: 20px;
  margin: 6px 0;
}

.message.assistant .message-content :deep(li) {
  margin: 4px 0;
}

.message.assistant .message-content :deep(blockquote) {
  border-left: 3px solid var(--border);
  padding-left: 12px;
  color: var(--text-muted);
  margin: 8px 0;
}

/* Typing cursor */
.typing-cursor {
  display: inline-block;
  width: 2px;
  height: 16px;
  background: var(--accent);
  margin-left: 2px;
  vertical-align: text-bottom;
  animation: cursor-blink 0.8s ease-in-out infinite;
}

@keyframes cursor-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

/* References */
.references {
  margin-top: 8px;
}

.references :deep(.el-collapse-item__header) {
  font-size: 12px;
  height: 28px;
  line-height: 28px;
  background: transparent;
  color: var(--text-muted);
}

.ref-header {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
}

.ref-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 0;
  font-size: 12px;
  line-height: 1.4;
}

.ref-index {
  color: var(--accent);
  font-weight: 700;
  flex-shrink: 0;
  font-size: 11px;
}

.ref-source {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 4px;
  background: var(--bg-muted);
  color: var(--text-muted);
  font-weight: 600;
  flex-shrink: 0;
}

.ref-doc {
  color: var(--text-secondary);
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ref-score {
  color: var(--text-muted);
  font-size: 11px;
  flex-shrink: 0;
  font-variant-numeric: tabular-nums;
}

/* Input area */
.input-area {
  padding: 16px 24px;
  border-top: 1px solid var(--border-subtle);
  background: var(--bg-elevated);
}

.chat-input :deep(.el-textarea__inner) {
  border-radius: var(--radius-sm);
  padding: 10px 14px;
  font-size: 14px;
  line-height: 1.5;
  border-color: var(--border);
}

.chat-input :deep(.el-textarea__inner):focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-muted);
}

.input-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}

.input-tip {
  font-size: 12px;
  color: var(--text-muted);
}

.send-btn {
  border-radius: var(--radius-sm) !important;
  font-weight: 600;
  font-size: 13px;
  height: 34px;
  padding: 0 16px;
}

@media (max-width: 768px) {
  .chat-container {
    flex-direction: column;
    height: auto;
  }
  .chat-sidebar {
    width: 100%;
    max-height: 200px;
    border-right: none;
    border-bottom: 1px solid var(--border-subtle);
  }
}
</style>
