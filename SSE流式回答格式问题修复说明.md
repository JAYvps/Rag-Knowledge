# SSE 流式回答格式显示异常 — 问题分析与修复

## 问题描述

在 AI 的 SSE（Server-Sent Events）流式回答过程中，AI 返回的 Markdown 内容无法正确显示格式（如标题、加粗、代码块等），表现为格式丢失或显示为原始 Markdown 文本。需要手动刷新网页或切换到其他页面再回到对话页面，才能正常显示格式。

---

## 根本原因

### 原因一：`currentEvent` 变量作用域错误（主要问题）

在 `frontend/src/api/chat.js` 的 SSE 解析逻辑中，`currentEvent` 变量被声明在 `read()` 函数内部：

```javascript
function read() {
    reader.read().then(({ done, value }) => {
        // ...
        let currentEvent = ''  // 每次 read() 调用都会重置为空字符串

        for (const line of lines) {
            if (line.startsWith('event:')) {
                currentEvent = line.substring(6).trim()
            } else if (line.startsWith('data:')) {
                // 根据 currentEvent 分发数据
            }
        }

        read()  // 递归调用，currentEvent 再次被重置
    })
}
```

**问题发生场景：**

SSE 事件的格式为：
```
event:content
data:"AI生成的内容"
```

当网络传输将一个 SSE 事件拆分到两个 chunk 中时：

- **Chunk N** 包含：`event:content\n`
- **Chunk N+1** 包含：`data:"Hello World"\n\n`

处理流程：
1. 处理 Chunk N → `currentEvent = 'content'`，无 data 行，事件未完成
2. 处理 Chunk N+1 → `currentEvent` 被**重置为空字符串 `''`** → `data:"Hello World"` 被解析后，switch 语句匹配不到任何 case → **该内容块被静默丢弃**

这导致 `streamingContent` 中丢失了部分文本片段，Markdown 渲染结果不完整，格式错乱。

### 原因二：代码块高亮函数返回空字符串（次要问题）

在 `frontend/src/views/chat/ChatView.vue` 中，MarkdownIt 的 `highlight` 配置：

```javascript
highlight(str, lang) {
    if (lang && hljs.getLanguage(lang)) {
        try {
            return hljs.highlight(str, {language: lang}).value
        } catch (_) {}
    }
    return ''  // ❌ 未识别的语言返回空字符串，导致代码块内容丢失
}
```

当 AI 生成的代码块未标注语言或使用了 highlight.js 不支持的语言时，`highlight` 函数返回空字符串，MarkdownIt 将其作为代码块的 HTML 内容，导致**代码块内容完全消失**。

---

## 修复方案

### 修复一：将 `currentEvent` 提升到外层作用域

将 `currentEvent` 的声明从 `read()` 函数内部移到外层闭包中，与 `buffer` 同级，使其在多次 `read()` 调用间保持状态：

```javascript
let buffer = ''
let currentEvent = ''  // ✅ 在 read() 外部声明，跨调用保持

function read() {
    reader.read().then(({ done, value }) => {
        // ...
        // 不再重置 currentEvent

        for (const line of lines) {
            if (line.startsWith('event:')) {
                currentEvent = line.substring(6).trim()
            } else if (line.startsWith('data:')) {
                // 正确使用上一次 read() 中设置的 currentEvent
            }
        }

        read()
    })
}
```

**修复效果：** 即使 chunk 边界拆分了 `event:` 行和 `data:` 行，`currentEvent` 在下一个 `read()` 中仍然保持正确的事件名称，内容不再丢失。

### 修复二：代码块高亮函数返回转义后的原文

将 `highlight` 函数的 fallback 返回值从空字符串改为 HTML 转义后的原文：

```javascript
highlight(str, lang) {
    if (lang && hljs.getLanguage(lang)) {
        try {
            return hljs.highlight(str, {language: lang}).value
        } catch (_) {}
    }
    return md.utils.escapeHtml(str)  // ✅ 返回转义后的原文，保留代码内容
}
```

**修复效果：** 未识别语言的代码块仍然正常显示代码内容（只是没有语法高亮），不再丢失。

---

## 修改文件清单

| 文件 | 修改内容 |
|------|----------|
| `frontend/src/api/chat.js` | 将 `let currentEvent = ''` 从 `read()` 内部移至外层闭包 |
| `frontend/src/views/chat/ChatView.vue` | `highlight` 函数 fallback 返回 `md.utils.escapeHtml(str)` |

---

## 问题复现条件

此问题在网络传输不稳定、chunk 边界恰好拆分 SSE 事件头和数据行时更容易出现，因此表现为偶发性。修复后无论 chunk 如何拆分，事件上下文都能正确保持。
