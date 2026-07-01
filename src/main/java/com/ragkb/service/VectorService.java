// ============ service/VectorService.java ============
package com.ragkb.service;

import com.ragkb.common.LocalVectorStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 向量数据库操作服务
 *
 * 底层使用LocalVectorStore（纯Java本地实现）
 * 提供存储、检索、删除的统一接口
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorService {

    private final LocalVectorStore vectorStore;

    /**
     * 存入向量
     *
     * @param content  文本内容
     * @param vector   向量（由EmbeddingService生成）
     * @param metadata 元数据（来源、文档名等）
     * @return 向量ID
     */
    public String store(String content, float[] vector, Map<String, String> metadata) {
        String id = vectorStore.add(content, metadata, vector);
        log.debug("向量已存入: id={}, contentLen={}", id, content.length());
        return id;
    }

    /**
     * 批量存入向量
     */
    public List<String> storeBatch(List<String> contents,
                                   List<float[]> vectors,
                                   List<Map<String, String>> metadatas) {
        List<String> ids = vectorStore.addBatch(contents, metadatas, vectors);
        log.info("批量向量存入: {} 条", ids.size());
        return ids;
    }

    /**
     * 向量检索
     *
     * @param question 用户问题
     * @param topK     返回数量
     * @return 检索结果
     */
    public List<VectorSearchResult> search(String question, float[] questionVector, int topK) {
        List<LocalVectorStore.SearchResult> rawResults =
                vectorStore.search(questionVector, topK, 0.5);

        List<VectorSearchResult> results = rawResults.stream()
                .map(r -> new VectorSearchResult(
                        r.getContent(),
                        r.getMetadata() != null ? new HashMap<>(r.getMetadata()) : new HashMap<>(),
                        r.getScore()
                ))
                .toList();

        log.info("向量检索: question='{}', topK={}, 返回={}条",
                question.substring(0, Math.min(50, question.length())),
                topK, results.size());

        return results;
    }

    /**
     * 按ID批量删除
     */
    public void deleteByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) return;
        vectorStore.delete(ids);
        log.info("向量删除: {} 条", ids.size());
    }

    /**
     * 获取向量总数
     */
    public int size() {
        return vectorStore.size();
    }

    /**
     * 检索结果
     */
    public record VectorSearchResult(
            String content,
            Map<String, String> metadata,
            double score
    ) {}
}
