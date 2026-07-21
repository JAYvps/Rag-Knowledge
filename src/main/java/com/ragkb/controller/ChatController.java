// ============ controller/ChatController.java ============
package com.ragkb.controller;

import com.ragkb.common.Result;
import com.ragkb.dto.ChatMessage;
import com.ragkb.dto.ConversationVO;
import com.ragkb.dto.RagRequest;
import com.ragkb.dto.SourceRef;
import com.ragkb.security.UserDetailsImpl;
import com.ragkb.service.RagService;
import com.ragkb.service.VectorService.VectorSearchResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final RagService ragService;
    private final ChatClient chatClient;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * RAG问答 - SSE流式返回
     */
    @PostMapping("/ask")
    public SseEmitter ask(@Valid @RequestBody RagRequest req,
                          @AuthenticationPrincipal UserDetailsImpl user) {
        if (user == null) {
            SseEmitter emitter = new SseEmitter();
            emitter.completeWithError(new RuntimeException("未登录"));
            return emitter;
        }

        SseEmitter emitter = new SseEmitter(120_000L);

        emitter.onTimeout(() -> {
            log.warn("SSE超时");
            emitter.complete();
        });
        emitter.onError(e -> log.warn("SSE错误: {}", e.getMessage()));

        // 用数组包装 convId，解决 lambda 中不能修改局部变量的问题
        final Long[] convIdHolder = new Long[1];

        executor.submit(() -> {
            try {
                // 1. 获取或创建对话
                convIdHolder[0] = ragService.getOrCreateConversationId(
                        req.getConversationId(), user.getUserId()
                );
                final Long convId = convIdHolder[0];

                // 2. 保存用户消息
                try {
                    ragService.saveUserMessage(convId, req.getQuestion());
                    log.info("[对话] 用户消息已保存: convId={}", convId);
                } catch (Exception e) {
                    log.error("[对话] 保存用户消息失败", e);
                }

                // 3. 向量检索（根据搜索范围过滤）
                List<VectorSearchResult> searchResults =
                        ragService.retrieveWithContent(req.getQuestion(), user.getUserId(), req.getSearchScope());

                // 4. 构建引用来源
                List<SourceRef> sources = ragService.buildSources(searchResults);
                sendSse(emitter, "references", sources);

                // 5. 构建上下文和Prompt
                String context = ragService.buildContext(searchResults);
                String systemPrompt = buildSystemPrompt(context);

                // 6. 组装用户消息（含对话历史）
                //    用实体类型的Message列表
                List<com.ragkb.entity.Message> history =
                        ragService.getEntityChatHistory(convId);
                String userMessage = buildUserMessage(history, req.getQuestion());

                // 7. 流式调用LLM
                StringBuilder fullResponse = new StringBuilder();

                chatClient.prompt()
                        .system(systemPrompt)
                        .user(userMessage)
                        .stream()
                        .content()
                        .doOnNext(chunk -> {
                            fullResponse.append(chunk);
                            sendSse(emitter, "content", chunk);
                        })
                        .blockLast();  // ← subscribe() 改成 blockLast()

                // 8. 流结束后，同步保存回复（不再放在doOnComplete里）
                try {
                    ragService.saveAssistantMessage(convId,
                            fullResponse.toString(), sources);
                    log.info("[对话] AI回复已保存: convId={}, len={}",
                            convId, fullResponse.length());
                } catch (Exception e) {
                    log.error("[对话] 保存AI回复失败", e);
                }

                // 9. 发送done事件 + 完成
                sendSse(emitter, "done", Map.of("conversationId", convId));

                // 延迟关闭，确保done事件刷到前端
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}

                try {
                    emitter.complete();
                } catch (Exception e) {
                    log.debug("[SSE] emitter.complete() 异常（可忽略）: {}", e.getMessage());
                }
            } catch (Exception e) {
                log.error("[对话] RAG问答失败", e);
                sendSse(emitter, "error", "问答失败: " + e.getMessage());
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    // ==================== 对话管理 ====================

    @PostMapping("/conversation")
    public Result<ConversationVO> createConversation(
            @AuthenticationPrincipal UserDetailsImpl user) {
        if (user == null) {
            return Result.fail("未登录或Token已过期");
        }
        var conv = ragService.createConversation(user.getUserId());
        ConversationVO vo = new ConversationVO();
        vo.setId(conv.getId());
        vo.setTitle(conv.getTitle());
        vo.setCreatedAt(conv.getCreatedAt());
        vo.setUpdatedAt(conv.getUpdatedAt());
        return Result.ok(vo);
    }

    @GetMapping("/conversations")
    public Result<List<ConversationVO>> listConversations(
            @AuthenticationPrincipal UserDetailsImpl user) {
        if (user == null) {
            return Result.fail("未登录或Token已过期");
        }
        return Result.ok(ragService.listConversations(user.getUserId()));
    }

    @GetMapping("/history/{conversationId}")
    public Result<List<ChatMessage>> getHistory(@PathVariable Long conversationId) {
        return Result.ok(ragService.getHistory(conversationId));
    }

    @DeleteMapping("/conversation/{id}")
    public Result<Void> deleteConversation(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl user) {
        if (user == null) {
            return Result.fail("未登录或Token已过期");
        }
        ragService.deleteConversation(id, user.getUserId());
        return Result.ok();
    }

    // ==================== 内部方法 ====================

//    private void sendSse(SseEmitter emitter, String eventName, Object data) {
//        try {
//            emitter.send(SseEmitter.event().name(eventName).data(data));
//        } catch (IOException | IllegalStateException e) {
//            log.warn("SSE推送失败: event={}, msg={}", eventName, e.getMessage());
//        }
//    }
private void sendSse(SseEmitter emitter, String eventName, Object data) {
    try {
        emitter.send(SseEmitter.event()
                .name(eventName)
                .data(data, MediaType.APPLICATION_JSON));
    } catch (Exception e) {
        log.debug("[SSE] send失败（流可能已关闭）: {}", e.getMessage());
    }
}

    /**
     * 组装用户消息（含对话历史）
     * 使用实体Message，通过 getRole() 和 getContent() 访问字段
     */
    private String buildUserMessage(List<com.ragkb.entity.Message> history,
                                    String question) {
        if (history.isEmpty()) {
            return question;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("以下是之前的对话记录：\n\n");
        for (com.ragkb.entity.Message msg : history) {
            String role = "user".equals(msg.getRole()) ? "用户" : "助手";
            sb.append(role).append(": ").append(msg.getContent()).append("\n\n");
        }
        sb.append("请回答用户最新的问题：\n").append(question);
        return sb.toString();
    }

    private String buildSystemPrompt(String context) {
        return """
            你是一个智能知识库助手。你的知识来自企业知识库和用户上传的文档。

            请严格根据下面的参考资料来回答用户的问题。

            规则：
            - 基于参考资料回答，引用时标注来源编号如[1][2]，同一来源最多引用1次
            - 如果资料中没有相关信息，坦诚告知并建议用户换个方式提问
            - 不要编造资料中不存在的内容
            - 回答要准确、条理清晰、易于理解
            - 如果资料来自不同文档，可以综合分析
            - 回答完成后，只列出真正使用到的来源。
            - 适当使用Markdown格式（标题、列表、加粗等）增强可读性

            【参考资料】
            %s
            """.formatted(context);
    }
}
