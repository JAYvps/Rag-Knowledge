// ============ src/api/chat.js ============
import request from './request'

export function createConversation() {
    return request.post('/chat/conversation')
}

export function getConversations() {
    return request.get('/chat/conversations')
}

export function getHistory(conversationId) {
    return request.get(`/chat/history/${conversationId}`)
}

export function deleteConversation(conversationId) {
    return request.delete(`/chat/conversation/${conversationId}`)
}

/**
 * RAG问答 - 使用原生fetch实现SSE
 *
 * SSE不能用axios（axios不支持text/event-stream）
 * 所以这里用fetch手动处理，手动带Token
 */
export function askQuestion(params, callbacks) {
    const controller = new AbortController()
    let doneReceived = false  // ← 新增

    fetch('/api/chat/ask', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + localStorage.getItem('token')
        },
        body: JSON.stringify(params),
        signal: controller.signal
    })
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`)
            }

            const reader = response.body.getReader()
            const decoder = new TextDecoder()
            let buffer = ''
            let currentEvent = ''

            function read() {
                reader.read().then(({ done, value }) => {
                    if (done) {
                        if (!doneReceived) {
                            callbacks.onDone?.({ conversationId: params.conversationId })
                        }
                        return
                    }

                    buffer += decoder.decode(value, { stream: true })
                    const lines = buffer.split('\n')
                    buffer = lines.pop() || ''

                    for (const line of lines) {
                        if (line.startsWith('event:')) {
                            currentEvent = line.substring(6).trim()
                        } else if (line.startsWith('data:')) {
                            const data = line.substring(5).trim()

                            try {
                                const parsed = JSON.parse(data)

                                switch (currentEvent) {
                                    case 'references':
                                        callbacks.onReferences?.(parsed)
                                        break
                                    case 'content':
                                        callbacks.onContent?.(parsed)
                                        break
                                    case 'done':
                                        doneReceived = true
                                        callbacks.onDone?.(parsed)
                                        break
                                    case 'error':
                                        callbacks.onError?.(parsed)
                                        break
                                }
                            } catch (e) {
                                if (currentEvent === 'content') {
                                    callbacks.onContent?.(data)
                                }
                            }
                        }
                    }

                    read()
                }).catch(err => {
                    if (err.name !== 'AbortError') {
                        callbacks.onError?.(err.message)
                    }
                })
            }

            read()
        })
        .catch(err => {
            if (err.name !== 'AbortError') {
                callbacks.onError?.(err.message)
            }
        })

    return controller
}
