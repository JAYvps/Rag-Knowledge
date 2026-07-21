// ============ service/RagService.java ============
package com.ragkb.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ragkb.dto.ChatMessage;
import com.ragkb.dto.ConversationVO;
import com.ragkb.dto.SourceRef;
import com.ragkb.entity.Conversation;
import com.ragkb.entity.Message;
import com.ragkb.mapper.ConversationMapper;
import com.ragkb.mapper.MessageMapper;
import com.ragkb.service.VectorService.VectorSearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * RAG问答核心服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final EmbeddingService embeddingService;
    private final VectorService vectorService;
    private final ChatClient chatClient;
    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final ObjectMapper objectMapper;

    private static final int TOP_K = 5;
    private static final int RETRIEVE_TOP_K = 10;
    private static final double MIN_SCORE = 0.5;
    private static final int HISTORY_LIMIT = 6;

    // ==================== 对话管理 ====================

    /**
     * 创建新对话
     */
    public Conversation createConversation(Long userId) {
        Conversation conv = new Conversation();
        conv.setUserId(userId);
        conv.setTitle("新对话");
        conversationMapper.insert(conv);
        return conv;
    }

    /**
     * 获取用户的对话列表
     */
    public List<ConversationVO> listConversations(Long userId) {
        List<Conversation> convs = conversationMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Conversation>()
                        .eq(Conversation::getUserId, userId)
                        .orderByDesc(Conversation::getUpdatedAt)
        );

        return convs.stream().map(c -> {
            ConversationVO vo = new ConversationVO();
            vo.setId(c.getId());
            vo.setTitle(c.getTitle());
            vo.setCreatedAt(c.getCreatedAt());
            vo.setUpdatedAt(c.getUpdatedAt());
            return vo;
        }).toList();
    }

    /**
     * 获取对话的消息历史
     */
    public List<ChatMessage> getHistory(Long conversationId) {
        List<Message> messages = messageMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Message>()
                        .eq(Message::getConversationId, conversationId)
                        .orderByAsc(Message::getCreatedAt)
        );

        return messages.stream().map(this::toChatMessage).toList();
    }

    /**
     * 删除对话（级联删除消息）
     */
    public void deleteConversation(Long conversationId, Long userId) {
        Conversation conv = conversationMapper.selectById(conversationId);
        if (conv == null || !conv.getUserId().equals(userId)) {
            throw new RuntimeException("对话不存在或无权操作");
        }
        messageMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Message>()
                        .eq(Message::getConversationId, conversationId)
        );
        conversationMapper.deleteById(conversationId);
    }

    /**
     * 获取或创建对话ID
     */
    public Long getOrCreateConversationId(Long conversationId, Long userId) {
        if (conversationId == null) {
            Conversation conv = createConversation(userId);
            return conv.getId();
        }
        return conversationId;
    }

    /**
     * 保存用户消息
     */
    public void saveUserMessage(Long conversationId, String question) {
        Message userMsg = new Message();
        userMsg.setConversationId(conversationId);
        userMsg.setRole("user");
        userMsg.setContent(question);
        messageMapper.insert(userMsg);

        log.info("[对话] 用户消息已入库: convId={}, msgId={}", conversationId, userMsg.getId());

        // 如果是第一条消息（id最小），异步生成标题
        Conversation conv = conversationMapper.selectById(conversationId);
        if (conv != null && "新对话".equals(conv.getTitle())) {
            asyncGenerateTitle(conversationId, question);
        }
    }

    /**
     * 获取对话历史（返回实体Message，供Controller构建上下文）
     * 区别于 getChatHistory 返回 Spring AI 的 Message 接口
     */
    public List<com.ragkb.entity.Message> getEntityChatHistory(Long conversationId) {
        return messageMapper.selectRecent(conversationId, HISTORY_LIMIT);
    }



    /**
     * 异步生成对话标题
     * 用LLM把用户问题浓缩成一个简短标题
     */
    private void asyncGenerateTitle(Long conversationId, String question) {
        new Thread(() -> {
            try {
                String title = chatClient.prompt()
                        .system("请把用户的问题浓缩成一个简短的标题（不超过15个字）。只输出标题，不要输出任何其他内容，不要加引号。")
                        .user(question)
                        .call()
                        .content();

                // 清理可能的引号和多余空白
                if (title != null) {
                    title = title.replaceAll("[\"'「」]", "").trim();
                    if (title.length() > 20) {
                        title = title.substring(0, 20);
                    }
                }

                if (title != null && !title.isBlank()) {
                    Conversation conv = conversationMapper.selectById(conversationId);
                    if (conv != null) {
                        conv.setTitle(title);
                        conversationMapper.updateById(conv);
                        log.info("对话标题已生成: convId={}, title={}", conversationId, title);
                    }
                }
            } catch (Exception e) {
                log.warn("生成对话标题失败: convId={}", conversationId, e);
            }
        }).start();
    }

    // ==================== RAG检索 ====================

    /**
     * 执行向量检索，获取引用来源
     *
     * @param question    用户问题
     * @param userId      当前用户ID（用于过滤个人文档）
     * @param searchScope 搜索范围 ("global" 或 "own")
     */
    public List<SourceRef> retrieve(String question, Long userId, String searchScope) {
        float[] questionVector = embeddingService.embed(question);
        List<VectorSearchResult> allResults = vectorService.search(question, questionVector, RETRIEVE_TOP_K);
        
        // 根据搜索范围过滤
        List<VectorSearchResult> filtered = filterByScope(allResults, userId, searchScope)
                .stream()
                .filter(r -> r.score() >= MIN_SCORE)
                .limit(TOP_K)
                .toList();
        
        return buildSources(filtered);
    }

    /**
     * 获取检索结果（带文本内容，用于构建上下文）
     * 动态策略：扩大检索范围，再按阈值截断，避免填充噪声引用
     *
     * @param question    用户问题
     * @param userId      当前用户ID（用于过滤个人文档）
     * @param searchScope 搜索范围 ("global" 或 "own")
     */
    public List<VectorSearchResult> retrieveWithContent(String question, Long userId, String searchScope) {
        float[] questionVector = embeddingService.embed(question);
        List<VectorSearchResult> allResults = vectorService.search(question, questionVector, RETRIEVE_TOP_K);

        // 根据搜索范围过滤
        List<VectorSearchResult> filtered = filterByScope(allResults, userId, searchScope)
                .stream()
                .filter(r -> r.score() >= MIN_SCORE)
                .limit(TOP_K)
                .toList();

        log.info("动态检索: question='{}', 召回={}条, 过滤后={}条, scope={}",
                question.substring(0, Math.min(30, question.length())),
                allResults.size(), filtered.size(), searchScope);

        return filtered;
    }

    /**
     * 根据搜索范围过滤检索结果
     *
     * 过滤规则：
     * - searchScope=global: 语雀文档 + 全局文档 + 个人文档
     * - searchScope=own: 仅个人文档
     */
    private List<VectorSearchResult> filterByScope(
            List<VectorSearchResult> results, Long userId, String searchScope) {
        
        if ("own".equals(searchScope)) {
            // 仅返回个人文档
            return results.stream()
                    .filter(r -> {
                        String sourceType = r.metadata().getOrDefault("sourceType", "");
                        String docUserId = r.metadata().getOrDefault("userId", "");
                        return "user".equals(sourceType) && String.valueOf(userId).equals(docUserId);
                    })
                    .toList();
        } else {
            // 默认/global: 语雀文档 + 全局文档 + 个人文档
            return results.stream()
                    .filter(r -> {
                        String sourceType = r.metadata().getOrDefault("sourceType", "");
                        if ("yuque".equals(sourceType)) {
                            return true; // 语雀文档始终可搜索
                        }
                        if ("user".equals(sourceType)) {
                            String isGlobal = r.metadata().getOrDefault("isGlobal", "false");
                            String docUserId = r.metadata().getOrDefault("userId", "");
                            // 全局文档或自己的文档
                            return "true".equals(isGlobal) || String.valueOf(userId).equals(docUserId);
                        }
                        return false;
                    })
                    .toList();
        }
    }

    /**
     * 构建上下文（检索到的参考资料文本）
     */
    public String buildContext(List<VectorSearchResult> results) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            VectorSearchResult r = results.get(i);
            Map<String, String> meta = r.metadata();

            String source;
            if ("yuque".equals(meta.getOrDefault("sourceType", ""))) {
                source = meta.getOrDefault("repoName", "") + " / "
                        + meta.getOrDefault("docTitle", "");
            } else {
                source = meta.getOrDefault("docTitle", "");
            }

            sb.append(String.format("[%d] 来源: %s\n%s\n\n",
                    i + 1, source, r.content()));
        }
        return sb.toString();
    }

    /**
     * 构建引用来源
     */
    public List<SourceRef> buildSources(List<VectorSearchResult> results) {
        List<SourceRef> sources = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            VectorSearchResult r = results.get(i);
            Map<String, String> meta = r.metadata();

            String sourceLabel;
            String docTitle;
            String sourceType = meta.getOrDefault("sourceType", "user");
            Long sourceId = null;
            Long repoId = null;
            Boolean isGlobal = false;

            if ("yuque".equals(sourceType)) {
                sourceLabel = "企业知识库";
                docTitle = meta.getOrDefault("repoName", "") + " / "
                        + meta.getOrDefault("docTitle", "");
                sourceId = parseLong(meta.get("sourceId"));
                repoId = parseLong(meta.get("repoId"));
            } else {
                // user 类型文档
                String globalFlag = meta.getOrDefault("isGlobal", "false");
                isGlobal = "true".equals(globalFlag);
                if (isGlobal) {
                    sourceLabel = "全局文档";
                } else {
                    sourceLabel = "我的文档";
                }
                docTitle = meta.getOrDefault("docTitle", meta.getOrDefault("fileName", ""));
                sourceId = parseLong(meta.get("sourceId"));
            }

            String snippet = r.content().length() > 200
                    ? r.content().substring(0, 200) + "..."
                    : r.content();

            SourceRef ref = new SourceRef();
            ref.setIndex(i + 1);
            ref.setSourceLabel(sourceLabel);
            ref.setDocTitle(docTitle);
            ref.setSnippet(snippet);
            ref.setScore(r.score());
            ref.setSourceType(sourceType);
            ref.setSourceId(sourceId);
            ref.setRepoId(repoId);
            ref.setIsGlobal(isGlobal);
            sources.add(ref);
        }
        return sources;
    }

    /**
     * 安全解析Long
     */
    private Long parseLong(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 获取对话历史（用于多轮对话上下文）
     */
    public List<org.springframework.ai.chat.messages.Message> getChatHistory(Long conversationId) {
        List<Message> recent = messageMapper.selectRecent(conversationId, HISTORY_LIMIT);

        return recent.stream()
                .map(m -> {
                    if ("user".equals(m.getRole())) {
                        return (org.springframework.ai.chat.messages.Message)
                                new UserMessage(m.getContent());
                    } else if ("assistant".equals(m.getRole())) {
                        return (org.springframework.ai.chat.messages.Message)
                                new AssistantMessage(m.getContent());
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 保存AI回复
     */
    public void saveAssistantMessage(Long conversationId, String content,
                                     List<SourceRef> references) {
        Message msg = new Message();
        msg.setConversationId(conversationId);
        msg.setRole("assistant");
        msg.setContent(content);
        msg.setCreatedAt(LocalDateTime.now());

        if (references != null && !references.isEmpty()) {
            try {
                msg.setRefData(objectMapper.writeValueAsString(references));
            } catch (JsonProcessingException e) {
                log.warn("序列化引用来源失败", e);
            }
        }

        messageMapper.insert(msg);
    }

    // ==================== 私有方法 ====================

    /**
     * Message实体转ChatMessage DTO
     */
    private ChatMessage toChatMessage(Message m) {
        ChatMessage cm = new ChatMessage();
        cm.setId(m.getId());
        cm.setConversationId(m.getConversationId());
        cm.setRole(m.getRole());
        cm.setContent(m.getContent());
        cm.setCreatedAt(m.getCreatedAt());

        // 解析引用来源
        if (m.getRefData() != null && !m.getRefData().isBlank()) {
            try {
                cm.setReferences(objectMapper.readValue(
                        m.getRefData(), new TypeReference<List<SourceRef>>() {}
                ));
            } catch (JsonProcessingException e) {
                cm.setReferences(List.of());
            }
        } else {
            cm.setReferences(List.of());
        }

        return cm;
    }
}
