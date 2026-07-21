// ============ service/YuqueSyncService.java ============
package com.ragkb.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ragkb.entity.DocumentChunk;
import com.ragkb.entity.YuqueDocSync;
import com.ragkb.entity.YuqueRepo;
import com.ragkb.mapper.DocumentChunkMapper;
import com.ragkb.mapper.YuqueDocSyncMapper;
import com.ragkb.mapper.YuqueRepoMapper;
import com.ragkb.yuque.YuqueClient;
import com.ragkb.yuque.YuqueMarkdownCleaner;
import com.ragkb.yuque.YuqueRateLimitException;
import com.ragkb.yuque.dto.YuqueDocDetail;
import com.ragkb.yuque.dto.YuqueRepoDto;
import com.ragkb.yuque.dto.YuqueTocItem;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import com.ragkb.common.TextChunkUtil;
import com.ragkb.common.TextChunkUtil.TextChunk;

/**
 * 语雀知识库同步服务
 *
 * 支持两种数据来源：
 * 1. OWN:   自己语雀账户中的知识库（通过discover接口自动发现）
 * 2. PUBLIC: 互联网上任意公开知识库（通过addPublic接口手动添加）
 *
 * 同步策略：增量同步
 * - 新文档：拉取正文 → 清理Markdown → 分块 → 存MySQL
 * - 已变更文档：重新拉取 → 删除旧分块 → 重新分块存储
 * - 已删除文档：删除分块记录
 *
 * 限流策略：
 * - 每次API调用间隔500ms
 * - 遇到429暂停60秒后重试
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class YuqueSyncService {

    private final YuqueClient yuqueClient;
    private final YuqueRepoMapper yuqueRepoMapper;
    private final YuqueDocSyncMapper yuqueDocSyncMapper;
    private final DocumentChunkMapper documentChunkMapper;

    private final EmbeddingService embeddingService;
    private final VectorService vectorService;


    /** API调用间隔（毫秒） */
    private static final long API_INTERVAL = 500;

    /** 遇到429时的等待时间（毫秒） */
    private static final long RATE_LIMIT_WAIT = 60_000;

    // ==================== 同步结果 ====================

    /**
     * 同步结果统计
     */
    @Data
    public static class SyncResult {
        private int created;
        private int updated;
        private int deleted;
        private int failed;
        private long costMs;

        public void incCreated() { created++; }
        public void incUpdated() { updated++; }
        public void incDeleted() { deleted++; }
        public void incFailed()  { failed++; }

        @Override
        public String toString() {
            return String.format("新增=%d, 更新=%d, 删除=%d, 失败=%d, 耗时=%dms",
                    created, updated, deleted, failed, costMs);
        }
    }

    // ==================== 文本分块 ====================

    /**
     * 分块记录
     */
    //public record TextChunk(String content, int tokenCount) {}

    /**
     * 文本分块
     *
     * 策略：
     * 1. 先按Markdown标题（# ## ###）切分
     * 2. 超长章节按段落继续切分
     * 3. 每块前加上文档标题作为上下文
     *
     * @param title 文档标题
     * @param text  清理后的纯文本
     * @return 分块列表
     */
//    public List<TextChunk> splitText(String title, String text) {
//        if (text == null || text.isBlank()) {
//            return List.of();
//        }
//
//        final int MAX_CHUNK = 500;
//        final int OVERLAP = 50;
//
//        List<TextChunk> chunks = new ArrayList<>();
//
//        // 第1步：按标题切分
//        List<String> sections = splitByHeading(text);
//
//        for (String section : sections) {
//            if (section.length() <= MAX_CHUNK) {
//                String content = prependTitle(title, section);
//                chunks.add(new TextChunk(content, content.length()));
//            } else {
//                // 超长章节，按段落再切
//                List<String> subs = splitByParagraph(section, MAX_CHUNK, OVERLAP);
//                for (String sub : subs) {
//                    String content = prependTitle(title, sub);
//                    chunks.add(new TextChunk(content, content.length()));
//                }
//            }
//        }
//
//        // 至少保留一块
//        if (chunks.isEmpty()) {
//            String content = prependTitle(title, text);
//            chunks.add(new TextChunk(content, content.length()));
//        }
//
//        return chunks;
//    }

    /**
     * 按Markdown标题行切分
     */
//    private List<String> splitByHeading(String text) {
//        String[] lines = text.split("\n");
//        List<String> sections = new ArrayList<>();
//        StringBuilder current = new StringBuilder();
//
//        for (String line : lines) {
//            if (line.matches("^#{1,6}\\s+.+")) {
//                if (current.length() > 0) {
//                    sections.add(current.toString().trim());
//                    current = new StringBuilder();
//                }
//            }
//            current.append(line).append("\n");
//        }
//
//        if (current.length() > 0) {
//            sections.add(current.toString().trim());
//        }
//
//        return sections;
//    }

    /**
     * 按段落切分（处理超长章节）
     */
//    private List<String> splitByParagraph(String text, int maxSize, int overlap) {
//        List<String> result = new ArrayList<>();
//        String[] paragraphs = text.split("\n\n+");
//        StringBuilder current = new StringBuilder();
//
//        for (String para : paragraphs) {
//            if (current.length() + para.length() + 2 > maxSize && current.length() > 0) {
//                result.add(current.toString().trim());
//                String tail = current.length() > overlap
//                        ? current.substring(current.length() - overlap)prependTitle : "";
//                current = new StringBuilder(tail);
//            }
//            current.append(para).append("\n\n");
//        }
//
//        if (current.length() > 0) {
//            result.add(current.toString().trim());
//        }
//
//        return result;
//    }

    /**
     * 给分块内容加上文档标题上下文
     */
//    private String prependTitle(String title, String content) {
//        if (title != null && !title.isBlank() && !content.startsWith("# ")) {
//            return "## " + title + "\n\n" + content;
//        }
//        return content;
//    }

    // ==================== 自动发现（OWN来源） ====================

    /**
     * 从自己的语雀账户中自动发现知识库
     *
     * 拉取账户下所有知识库，把尚未入库的加入yuque_repo表
     * 只添加公开知识库
     *
     * @param ownNamespace 自己的语雀用户名或组织名
     * @return 新增数量
     */
    public int discoverOwnRepos(String ownNamespace) {
        log.info("[发现] 开始发现语雀知识库: namespace={}", ownNamespace);

        // 从语雀拉取知识库列表
        List<YuqueRepoDto> remoteRepos = yuqueClient.listRepos(ownNamespace);

        // 获取本地已有的（按 namespace+slug 去重）
        Set<String> existingKeys = yuqueRepoMapper.selectList(null)
                .stream()
                .map(r -> r.getNamespace() + "/" + r.getYuqueRepoSlug())
                .collect(Collectors.toSet());

        int added = 0;
        for (YuqueRepoDto dto : remoteRepos) {
            // 跳过私有库
            if (dto.getIsPublic() != null && dto.getIsPublic() != 1) {
                log.info("[发现] 跳过私有库: {}", dto.getName());
                continue;
            }

            String key = ownNamespace + "/" + dto.getSlug();
            if (existingKeys.contains(key)) {
                continue;
            }

            // 新知识库，入库
            YuqueRepo repo = new YuqueRepo();
            repo.setYuqueRepoId(dto.getId());
            repo.setYuqueRepoSlug(dto.getSlug());
            repo.setName(dto.getName());
            repo.setDescription(dto.getDescription());
            repo.setNamespace(ownNamespace);
            repo.setRepoSource("OWN");
            repo.setSyncStatus(0);
            repo.setDocCount(dto.getItemsCount() != null ? dto.getItemsCount() : 0);
            repo.setChunkCount(0);
            repo.setSortOrder(0);
            repo.setStatus(1);
            yuqueRepoMapper.insert(repo);
            added++;

            log.info("[发现] 新增: {} (slug={})", dto.getName(), dto.getSlug());
        }

        log.info("[发现] 完成，新增 {} 个", added);
        return added;
    }

    // ==================== 手动添加公开库（PUBLIC来源） ====================

    /**
     * 添加互联网上的公开知识库
     *
     * 适用于：别人的公开库、你个人账户的公开库
     * 验证方式：尝试拉取TOC，能访问就入库
     *
     * @param namespace 外部用户的语雀用户名
     * @param slug      知识库slug
     * @param name      展示名称（可选）
     * @return 入库记录
     */
    public YuqueRepo addPublicRepo(String namespace, String slug, String name) {
        // 检查是否已存在
        Long exists = yuqueRepoMapper.selectCount(
                new LambdaQueryWrapper<YuqueRepo>()
                        .eq(YuqueRepo::getNamespace, namespace)
                        .eq(YuqueRepo::getYuqueRepoSlug, slug)
        );
        if (exists > 0) {
            throw new RuntimeException("该知识库已添加: " + namespace + "/" + slug);
        }

        log.info("[添加] 验证公开知识库: {}/{}", namespace, slug);

        // 1. 尝试拉取TOC，验证可访问性
        List<YuqueTocItem> toc;
        try {
            toc = yuqueClient.getRepoToc(namespace, slug);
        } catch (Exception e) {
            throw new RuntimeException(
                    "无法访问该知识库，请检查namespace和slug: " + e.getMessage(), e);
        }

        // 2. 从listRepos中获取知识库ID和名称
        Long repoId = null;
        String repoName = name;
        try {
            List<YuqueRepoDto> repos = yuqueClient.listRepos(namespace);
            for (YuqueRepoDto dto : repos) {
                if (dto.getSlug().equals(slug)) {
                    repoId = dto.getId();
                    if (repoName == null || repoName.isBlank()) {
                        repoName = dto.getName();
                    }
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("[添加] 获取知识库ID失败，不影响入库: {}", e.getMessage());
        }

        if (repoName == null || repoName.isBlank()) {
            repoName = slug;
        }

        // 3. 统计文档数
        long docCount = toc.stream()
                .filter(item -> item.getDocId() != null)
                .count();

        // 4. 入库
        YuqueRepo repo = new YuqueRepo();
        repo.setYuqueRepoId(repoId);    // 可能为null，已允许
        repo.setYuqueRepoSlug(slug);
        repo.setName(repoName);
        repo.setNamespace(namespace);
        repo.setRepoSource("PUBLIC");
        repo.setSyncStatus(0);
        repo.setDocCount((int) docCount);
        repo.setChunkCount(0);
        repo.setSortOrder(99);
        repo.setStatus(1);
        yuqueRepoMapper.insert(repo);

        log.info("[添加] 已入库: {} ({}/{}) 文档数={}", repoName, namespace, slug, docCount);
        return repo;
    }

    /**
     * 查找已有知识库，不存在则新建
     */
    public YuqueRepo findOrCreateRepo(String namespace, String slug, String name) {
        // 先查是否已存在
        YuqueRepo existing = yuqueRepoMapper.selectOne(
                new LambdaQueryWrapper<YuqueRepo>()
                        .eq(YuqueRepo::getNamespace, namespace)
                        .eq(YuqueRepo::getYuqueRepoSlug, slug)
        );
        if (existing != null) {
            return existing;
        }
        return addPublicRepo(namespace, slug, name);
    }

    /**
     * 同步单篇文章
     *
     * @param repoId      yuque_repo表的ID
     * @param articleSlug 文章URL标识（如 uwohuv6bh94xqcr7）
     * @return 同步结果
     */
    public SyncResult syncSingleDoc(Long repoId, String articleSlug) {
        YuqueRepo repo = yuqueRepoMapper.selectById(repoId);
        if (repo == null) {
            throw new RuntimeException("知识库不存在: id=" + repoId);
        }

        log.info("===== 单篇同步: {}/{} 文章={} =====",
                repo.getNamespace(), repo.getYuqueRepoSlug(), articleSlug);

        // 1. 获取目录树，找到目标文章
        List<YuqueTocItem> toc = yuqueClient.getRepoToc(
                repo.getNamespace(), repo.getYuqueRepoSlug()
        );
        YuqueTocItem target = toc.stream()
                .filter(item -> item.getDocId() != null
                        && articleSlug.equals(item.getSlug()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "找不到文章: " + articleSlug + "，请检查slug是否正确"));

        // 2. 检查是否已同步过
        YuqueDocSync existing = yuqueDocSyncMapper.selectOne(
                new LambdaQueryWrapper<YuqueDocSync>()
                        .eq(YuqueDocSync::getYuqueRepoId, repoId)
                        .eq(YuqueDocSync::getYuqueDocId, target.getDocId())
        );

        // 3. 拉取 → 清理 → 分块 → 向量化
        SyncResult result = new SyncResult();
        try {
            repo.setSyncStatus(1);
            repo.setLastSyncMsg("同步中...");
            yuqueRepoMapper.updateById(repo);

            if (existing == null) {
                pullAndIndex(repo, target, null);
                result.incCreated();
            } else {
                pullAndIndex(repo, target, existing);
                result.incUpdated();
            }

            // 4. 更新统计
            repo.setChunkCount(countChunks(repoId));
            repo.setDocCount(Math.toIntExact(yuqueDocSyncMapper.selectCount(
                    new LambdaQueryWrapper<YuqueDocSync>()
                            .eq(YuqueDocSync::getYuqueRepoId, repoId)
                            .eq(YuqueDocSync::getSyncStatus, 1)
            )));
            repo.setSyncStatus(2);
            repo.setLastSyncAt(LocalDateTime.now());
            repo.setLastSyncMsg("单篇同步完成: " + target.getTitle());
            yuqueRepoMapper.updateById(repo);

            log.info("===== 单篇同步完成: {} =====", target.getTitle());

        } catch (Exception e) {
            log.error("单篇同步失败: {}", articleSlug, e);
            result.incFailed();
            repo.setSyncStatus(3);
            repo.setLastSyncMsg("同步失败: " + e.getMessage());
            yuqueRepoMapper.updateById(repo);
        }

        return result;
    }

    /**
     * 在功能模块删除知识库时，我们希望将知识库对应的文档和向量以及分块一起删除
     * 删除知识库（含所有文档的向量和分块）
     */
    public void deleteRepo(Long repoId) {
        YuqueRepo repo = yuqueRepoMapper.selectById(repoId);
        if (repo == null) {
            throw new RuntimeException("知识库不存在: id=" + repoId);
        }

        // 1. 删除该库下所有文档的向量和分块
        List<YuqueDocSync> docs = yuqueDocSyncMapper.selectByRepoId(repoId);
        for (YuqueDocSync doc : docs) {
            deleteDocChunksAndVectors(doc.getId());
        }

        // 2. 删除所有文档同步记录
        yuqueDocSyncMapper.delete(
                new LambdaQueryWrapper<YuqueDocSync>()
                        .eq(YuqueDocSync::getYuqueRepoId, repoId)
        );

        // 3. 删除知识库记录
        yuqueRepoMapper.deleteById(repoId);

        log.info("[删除] 已删除知识库: {} (id={})", repo.getName(), repoId);
    }



    // ==================== 同步单个知识库 ====================

    /**
     * 同步单个知识库（增量）
     *
     * @param repoId yuque_repo表的ID
     * @return 同步结果
     */
    public SyncResult syncRepo(Long repoId) {
        YuqueRepo repo = yuqueRepoMapper.selectById(repoId);
        if (repo == null) {
            throw new RuntimeException("知识库不存在: id=" + repoId);
        }

        log.info("===== 开始同步: {} ({}/{}) =====",
                repo.getName(), repo.getNamespace(), repo.getYuqueRepoSlug());

        // 标记同步中
        repo.setSyncStatus(1);
        repo.setLastSyncMsg("同步中...");
        yuqueRepoMapper.updateById(repo);

        SyncResult result = new SyncResult();
        long startTime = System.currentTimeMillis();

        try {
            // 1. 获取语雀目录树
            List<YuqueTocItem> toc = yuqueClient.getRepoToc(
                    repo.getNamespace(), repo.getYuqueRepoSlug()
            );

            // 只保留文档节点
            List<YuqueTocItem> docItems = toc.stream()
                    .filter(item -> item.getDocId() != null)
                    .toList();
            log.info("[同步] 语雀文档数: {}", docItems.size());

            // 2. 获取本地已有记录
            Map<Long, YuqueDocSync> localDocs = yuqueDocSyncMapper
                    .selectByRepoId(repoId)
                    .stream()
                    .collect(Collectors.toMap(
                            YuqueDocSync::getYuqueDocId,
                            doc -> doc,
                            (a, b) -> a
                    ));
            log.info("[同步] 本地已有: {}", localDocs.size());

            // 3. 遍历处理
            Set<Long> remoteDocIds = new HashSet<>();

            for (YuqueTocItem item : docItems) {
                remoteDocIds.add(item.getDocId());
                YuqueDocSync local = localDocs.get(item.getDocId());

                try {
                    if (local == null) {
                        // 新文档
                        pullAndIndex(repo, item, null);
                        result.incCreated();
                        log.info("[同步] 新增: {}", item.getTitle());
                    } else if (checkNeedUpdate(repo, item, local)) {
                        // 有变更
                        pullAndIndex(repo, item, local);
                        result.incUpdated();
                        log.info("[同步] 更新: {}", item.getTitle());
                    }
                    // else: 未变更，跳过

                } catch (YuqueRateLimitException e) {
                    log.warn("[同步] 频率限制，等待60秒后重试: {}", item.getTitle());
                    safeSleep(RATE_LIMIT_WAIT);
                    try {
                        pullAndIndex(repo, item, local);
                        if (local == null) result.incCreated();
                        else result.incUpdated();
                    } catch (Exception retryEx) {
                        log.error("[同步] 重试失败: {}", item.getTitle(), retryEx);
                        result.incFailed();
                    }
                } catch (Exception e) {
                    log.error("[同步] 处理失败: {}", item.getTitle(), e);
                    result.incFailed();
                }
            }

            // 4. 处理已删除的文档
            for (YuqueDocSync local : localDocs.values()) {
                if (!remoteDocIds.contains(local.getYuqueDocId())) {
                    deleteDocIndex(local);
                    result.incDeleted();
                    log.info("[同步] 删除: {}", local.getTitle());
                }
            }

            // 5. 更新状态
            result.setCostMs(System.currentTimeMillis() - startTime);
            repo.setSyncStatus(2);
            repo.setLastSyncAt(LocalDateTime.now());
            repo.setLastSyncMsg("同步完成: " + result);
            repo.setDocCount(docItems.size());
            repo.setChunkCount(countChunks(repoId));
            yuqueRepoMapper.updateById(repo);

            log.info("===== 同步完成: {} {} =====", repo.getName(), result);

        } catch (Exception e) {
            log.error("[同步] 知识库同步失败: {}", repo.getName(), e);
            result.setCostMs(System.currentTimeMillis() - startTime);
            repo.setSyncStatus(3);
            repo.setLastSyncAt(LocalDateTime.now());
            repo.setLastSyncMsg("同步失败: " + e.getMessage());
            yuqueRepoMapper.updateById(repo);
        }

        return result;
    }

    /**
     * 同步所有启用的知识库
     */
    public Map<String, SyncResult> syncAll() {
        log.info("===== 开始同步所有知识库 =====");
        List<YuqueRepo> repos = yuqueRepoMapper.selectEnabled();
        Map<String, SyncResult> results = new LinkedHashMap<>();

        for (YuqueRepo repo : repos) {
            try {
                SyncResult result = syncRepo(repo.getId());
                results.put(repo.getName(), result);
            } catch (Exception e) {
                log.error("[同步] {} 异常", repo.getName(), e);
            }
        }

        log.info("===== 全部同步完成 =====");
        return results;
    }

    /**
     * 定时同步（每天凌晨3点）
     */
    @Scheduled(cron = "${yuque.sync.cron:0 0 3 * * ?}")
    public void scheduledSync() {
        log.info("[定时] 开始自动同步");
        try {
            syncAll();
        } catch (Exception e) {
            log.error("[定时] 自动同步异常", e);
        }
    }

    // ==================== 内部方法 ====================

    /**
     * 拉取单篇文档 → 清理 → 分块 → 存储
     */
    /**
     * 拉取单篇文档 → 清理 → 分块 → 向量化 → 存储
     */
    private void pullAndIndex(YuqueRepo repo, YuqueTocItem item,
                              YuqueDocSync existing) {
        safeSleep(API_INTERVAL);

        // 1. 拉取文档正文
        YuqueDocDetail detail = yuqueClient.getDocDetail(
                repo.getNamespace(), repo.getYuqueRepoSlug(), item.getDocId()
        );

        String body = detail.getBody();
        if (body == null || body.isBlank()) {
            log.warn("[处理] 正文为空，跳过: {}", item.getTitle());
            return;
        }

        // 2. 计算MD5
        String contentHash = md5(body);

        // 3. 如果是更新，先删除旧分块和旧向量
        if (existing != null) {
            deleteDocChunksAndVectors(existing.getId());
        }

        // 4. 清理Markdown
        String cleanText = YuqueMarkdownCleaner.clean(body);

        // 5. 分块
        List<TextChunk> chunks = TextChunkUtil.split(detail.getTitle(), cleanText);

        // 6. 保存同步记录
        YuqueDocSync docSync = (existing != null) ? existing : new YuqueDocSync();
        if (existing == null) {
            docSync.setYuqueRepoId(repo.getId());
            docSync.setYuqueDocId(item.getDocId());
        }
        docSync.setTitle(detail.getTitle());
        docSync.setContentHash(contentHash);
        docSync.setWordCount(detail.getWordCount() != null
                ? detail.getWordCount() : cleanText.length());
        docSync.setChunkCount(chunks.size());
        docSync.setSyncStatus(1);
        docSync.setYuqueUpdatedAt(LocalDateTime.now());

        if (existing != null) {
            yuqueDocSyncMapper.updateById(docSync);
        } else {
            yuqueDocSyncMapper.insert(docSync);
        }

        // 7. 批量向量化 + 存入向量库 + 保存MySQL
        if (!chunks.isEmpty()) {
            List<String> texts = chunks.stream()
                    .map(TextChunk::content)
                    .toList();

            List<float[]> vectors = embeddingService.embedBatch(texts);

            for (int i = 0; i < chunks.size(); i++) {
                TextChunk chunk = chunks.get(i);

                // 构建元数据
                Map<String, String> metadata = new HashMap<>();
                metadata.put("sourceType", "yuque");
                metadata.put("sourceId", String.valueOf(docSync.getId()));
                metadata.put("docTitle", detail.getTitle());
                metadata.put("repoName", repo.getName());
                metadata.put("repoSlug", repo.getYuqueRepoSlug());
                metadata.put("repoId", String.valueOf(repo.getId()));
                metadata.put("chunkIndex", String.valueOf(i));

                // 存入向量库
                String vectorId = vectorService.store(
                        chunk.content(), vectors.get(i), metadata
                );

                // 保存MySQL分块记录
                DocumentChunk record = new DocumentChunk();
                record.setSourceType("yuque");
                record.setSourceId(docSync.getId());
                record.setChunkIndex(i);
                record.setContent(chunk.content());
                record.setTokenCount(chunk.tokenCount());
                record.setVectorId(vectorId);
                documentChunkMapper.insert(record);
            }

            log.info("[处理] 完成: {} 分块={}, 向量={}",
                    detail.getTitle(), chunks.size(), vectors.size());
        }
    }

    /**
     * 删除文档的分块 + 向量
     */
    private void deleteDocChunksAndVectors(Long docSyncId) {
        // 1. 查出所有分块的vectorId
        List<DocumentChunk> chunks = documentChunkMapper.selectBySource("yuque", docSyncId);
        List<String> vectorIds = chunks.stream()
                .map(DocumentChunk::getVectorId)
                .filter(Objects::nonNull)
                .toList();

        // 2. 从向量库中删除
        if (!vectorIds.isEmpty()) {
            vectorService.deleteByIds(vectorIds);
        }

        // 3. 从MySQL中删除
        documentChunkMapper.deleteBySource("yuque", docSyncId);
    }

    /**
     * 删除文档索引（分块 + 向量 + 同步记录）
     */
    private void deleteDocIndex(YuqueDocSync doc) {
        deleteDocChunksAndVectors(doc.getId());
        yuqueDocSyncMapper.deleteById(doc.getId());
    }


    /**
     * 检查文档是否需要更新
     */
    private boolean checkNeedUpdate(YuqueRepo repo, YuqueTocItem item,
                                    YuqueDocSync local) {
        try {
            safeSleep(API_INTERVAL);
            YuqueDocDetail detail = yuqueClient.getDocDetail(
                    repo.getNamespace(), repo.getYuqueRepoSlug(), item.getDocId()
            );
            String remoteHash = md5(detail.getBody());
            return !remoteHash.equals(local.getContentHash());
        } catch (Exception e) {
            log.warn("[同步] 检查变更失败，默认需要更新: {}", item.getTitle());
            return true;
        }
    }

    /**
     * 删除文档的所有分块
     */
    private void deleteDocChunks(Long docSyncId) {
        documentChunkMapper.delete(
                new LambdaQueryWrapper<DocumentChunk>()
                        .eq(DocumentChunk::getSourceType, "yuque")
                        .eq(DocumentChunk::getSourceId, docSyncId)
        );
    }


    /**
     * 统计知识库总分块数
     */
    private int countChunks(Long repoId) {
        List<Long> docIds = yuqueDocSyncMapper.selectByRepoId(repoId)
                .stream()
                .map(YuqueDocSync::getId)
                .toList();

        if (docIds.isEmpty()) return 0;

        Long count = documentChunkMapper.selectCount(
                new LambdaQueryWrapper<DocumentChunk>()
                        .eq(DocumentChunk::getSourceType, "yuque")
                        .in(DocumentChunk::getSourceId, docIds)
        );
        return count.intValue();
    }

//    private int countChunks(Long repoId) {
//        List<Long> docIds = yuqueDocSyncMapper.selectByRepoId(repoId)
//                .stream()
//                .map(YuqueDocSync::getId)
//                .toList();
//
//        if (docIds.isEmpty()) return 0;
//
//        return (int) documentChunkMapper.selectCount(
//                new LambdaQueryWrapper<DocumentChunk>()
//                        .eq(DocumentChunk::getSourceType, "yuque")
//                        .in(DocumentChunk::getSourceId, docIds)
//        );
//    }

    /**
     * 计算MD5
     */
    private String md5(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(content.hashCode());
        }
    }

    /**
     * 安全休眠
     */
    private void safeSleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
