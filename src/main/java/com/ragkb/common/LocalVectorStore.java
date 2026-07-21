// ============ common/LocalVectorStore.java ============
package com.ragkb.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * todo 后期转线上 注意修改config/VectorStoreConfig.java
 * 纯Java本地向量存储引擎
 * 不依赖任何外部向量数据库，所有向量存在内存中，持久化到本地JSON文件
 *
 * 原理：
 * - 每条数据存储：文本内容 + 元数据 + 向量（float数组）
 * - 检索时：将查询文本的向量与所有向量计算余弦相似度，返回最相似的TopK条
 * - 持久化：每次增删后序列化为JSON文件，启动时加载
 *
 * 性能：
 * - 人工智能知识库共计92个分块：检索耗时 < 1ms
 * - 1000个分块：检索耗时 < 5ms
 * - 10000个分块：检索耗时 < 50ms
 * 对于中小规模知识库完全够用
 *
 * 线程安全：使用读写锁，支持并发读、互斥写
 */
@Slf4j
public class LocalVectorStore {

    // ==================== 内部数据结构 ====================

    /**
     * 向量条目
     */
    @Data
    public static class VectorEntry {
        /** 唯一ID */
        private String id;
        /** 文本内容 */
        private String content;
        /** 元数据（来源、文档名等） */
        private Map<String, String> metadata;
        /** 向量（浮点数组） */
        private float[] vector;

        public VectorEntry() {}

        public VectorEntry(String id, String content,
                           Map<String, String> metadata, float[] vector) {
            this.id = id;
            this.content = content;
            this.metadata = metadata != null ? metadata : new HashMap<>();
            this.vector = vector;
        }
    }

    /**
     * 检索结果
     */
    @Data
    public static class SearchResult {
        /** 文本内容 */
        private String content;
        /** 元数据 */
        private Map<String, String> metadata;
        /** 余弦相似度分数（0~1，越高越相关） */
        private double score;

        public SearchResult(String content, Map<String, String> metadata, double score) {
            this.content = content;
            this.metadata = metadata;
            this.score = score;
        }
    }

    // ==================== 存储 ====================

    /** 所有向量条目 */
    private final List<VectorEntry> entries = new ArrayList<>();

    /** ID索引（快速查找） */
    private final Map<String, Integer> idIndex = new HashMap<>();

    /** 读写锁（并发安全） */
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /** 持久化文件路径 */
    private final String filePath;

    /** JSON序列化器 */
    private final ObjectMapper objectMapper;

    // ==================== 构造 ====================

    public LocalVectorStore(String storePath) {
        this.filePath = storePath + "/vector_store.json";
        this.objectMapper = new ObjectMapper();

        // 确保目录存在
        try {
            Files.createDirectories(Path.of(storePath));
        } catch (IOException e) {
            log.error("创建向量存储目录失败: {}", storePath, e);
        }

        // 启动时加载已有数据
        loadFromFile();
    }

    // ==================== 写操作 ====================

    /**
     * 添加单条向量
     *
     * @param content  文本内容
     * @param metadata 元数据
     * @param vector   向量
     * @return 生成的ID
     */
    public String add(String content, Map<String, String> metadata, float[] vector) {
        String id = UUID.randomUUID().toString().replace("-", "");
        VectorEntry entry = new VectorEntry(id, content, metadata, vector);

        lock.writeLock().lock();
        try {
            int index = entries.size();
            entries.add(entry);
            idIndex.put(id, index);
            saveToFile();
        } finally {
            lock.writeLock().unlock();
        }

        log.debug("向量已添加: id={}, contentLen={}", id, content.length());
        return id;
    }

    /**
     * 批量添加向量
     *
     * @param contents  文本列表
     * @param metadatas 元数据列表
     * @param vectors   向量列表
     * @return 生成的ID列表
     */
    public List<String> addBatch(List<String> contents,
                                 List<Map<String, String>> metadatas,
                                 List<float[]> vectors) {
        List<String> ids = new ArrayList<>();

        lock.writeLock().lock();
        try {
            for (int i = 0; i < contents.size(); i++) {
                String id = UUID.randomUUID().toString().replace("-", "");
                VectorEntry entry = new VectorEntry(
                        id,
                        contents.get(i),
                        metadatas.get(i),
                        vectors.get(i)
                );
                int index = entries.size();
                entries.add(entry);
                idIndex.put(id, index);
                ids.add(id);
            }
            saveToFile();
        } finally {
            lock.writeLock().unlock();
        }

        log.info("批量向量已添加: {} 条", ids.size());
        return ids;
    }

    /**
     * 按ID删除向量
     */
    public void delete(List<String> ids) {
        if (ids == null || ids.isEmpty()) return;

        lock.writeLock().lock();
        try {
            Set<String> deleteIds = new HashSet<>(ids);
            entries.removeIf(entry -> deleteIds.contains(entry.getId()));
            rebuildIndex();
            saveToFile();
        } finally {
            lock.writeLock().unlock();
        }

        log.info("向量已删除: {} 条", ids.size());
    }

    /**
     * 清空所有向量
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            entries.clear();
            idIndex.clear();
            saveToFile();
        } finally {
            lock.writeLock().unlock();
        }

        log.info("向量已清空");
    }

    /**
     * 获取总条数
     */
    public int size() {
        lock.readLock().lock();
        try {
            return entries.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 获取向量条目的元数据
     *
     * @param id 向量ID
     * @return 元数据副本，不存在返回null
     */
    public Map<String, String> getMetadata(String id) {
        lock.readLock().lock();
        try {
            Integer index = idIndex.get(id);
            if (index == null || index >= entries.size()) {
                return null;
            }
            VectorEntry entry = entries.get(index);
            return entry.getMetadata() != null ? new HashMap<>(entry.getMetadata()) : new HashMap<>();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 更新向量条目的元数据
     *
     * @param id       向量ID
     * @param metadata 新的元数据
     */
    public void updateMetadata(String id, Map<String, String> metadata) {
        lock.writeLock().lock();
        try {
            Integer index = idIndex.get(id);
            if (index == null || index >= entries.size()) {
                log.warn("向量不存在: id={}", id);
                return;
            }
            VectorEntry entry = entries.get(index);
            entry.setMetadata(metadata);
            saveToFile();
            log.debug("向量元数据已更新: id={}", id);
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ==================== 检索操作 ====================

    /**
     * 向量相似度检索
     *
     * 算法：余弦相似度
     * cosine(a, b) = (a · b) / (|a| * |b|)
     *
     * 结果按相似度降序排列，返回TopK条
     *
     * @param queryVector 查询向量
     * @param topK        返回数量
     * @param threshold   最低相似度阈值（0~1），低于此值的结果不返回
     * @return 检索结果列表
     */
    public List<SearchResult> search(float[] queryVector, int topK, double threshold) {
        lock.readLock().lock();
        try {
            if (entries.isEmpty()) {
                return List.of();
            }

            // 用优先队列（小顶堆）维护TopK
            PriorityQueue<SearchResult> heap = new PriorityQueue<>(
                    Comparator.comparingDouble(SearchResult::getScore)
            );

            for (VectorEntry entry : entries) {
                double score = cosineSimilarity(queryVector, entry.getVector());

                if (score < threshold) continue;

                heap.offer(new SearchResult(entry.getContent(), entry.getMetadata(), score));

                if (heap.size() > topK) {
                    heap.poll(); // 去掉分数最低的
                }
            }

            // 按分数降序输出
            List<SearchResult> results = new ArrayList<>(heap);
            results.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

            return results;

        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 带默认参数的检索
     */
    public List<SearchResult> search(float[] queryVector, int topK) {
        return search(queryVector, topK, 0.0);
    }

    // ==================== 余弦相似度计算 ====================

    /**
     * 计算两个向量的余弦相似度
     *
     * 公式: cos(a,b) = (a · b) / (|a| × |b|)
     *
     * 返回值范围: -1 到 1
     * - 1: 完全相同方向（语义完全一致）
     * - 0: 正交（语义无关）
     * - -1: 完全相反（语义相反）
     *
     * 实际使用中，文本Embedding的相似度通常在 0.5~0.95 之间
     */
    private double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) {
            return 0.0;
        }

        double dotProduct = 0.0;   // 点积
        double normA = 0.0;        // 向量a的模
        double normB = 0.0;        // 向量b的模

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    // ==================== 文件持久化 ====================

    /**
     * 保存到文件
     * 将内存中的所有向量条目序列化为JSON文件
     */
    private void saveToFile() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(filePath), entries);
            log.debug("向量数据已保存: {} 条, 文件={}", entries.size(), filePath);
        } catch (IOException e) {
            log.error("保存向量数据失败: {}", filePath, e);
        }
    }

    /**
     * 从文件加载
     * 启动时调用，恢复上次的数据
     */
    private void loadFromFile() {
        File file = new File(filePath);
        if (!file.exists()) {
            log.info("向量数据文件不存在，从空开始: {}", filePath);
            return;
        }

        try {
            List<VectorEntry> loaded = objectMapper.readValue(
                    file,
                    new TypeReference<List<VectorEntry>>() {}
            );

            entries.clear();
            entries.addAll(loaded);
            rebuildIndex();

            log.info("向量数据已加载: {} 条, 文件={}", entries.size(), filePath);

        } catch (IOException e) {
            log.error("加载向量数据失败: {}", filePath, e);
        }
    }

    /**
     * 重建ID索引
     * 删除操作后需要重建，因为元素位置会变化
     */
    private void rebuildIndex() {
        idIndex.clear();
        for (int i = 0; i < entries.size(); i++) {
            idIndex.put(entries.get(i).getId(), i);
        }
    }
}
